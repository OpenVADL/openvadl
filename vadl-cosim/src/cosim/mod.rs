use std::{
    collections::VecDeque,
    mem::ManuallyDrop,
    sync::{Arc, RwLock},
};

use anyhow::{Result, bail};
use serde::Serialize;
use tracing::debug;

use crate::{
    config::Config,
    diff::{DiffContextClient, DiffEntry, Report, diff::diff_cpus},
    ipc::{
        cstructs::{BrokerSHMExec, BrokerSHMTB},
        qemu::Client,
    },
};

#[derive(Debug, Serialize)]
pub enum TraceData {
    TB(Box<BrokerSHMTB>),
    Exec(Box<BrokerSHMExec>),
}

#[derive(Debug)]
pub struct BoundedVecDeque<T> {
    pub deque: VecDeque<T>,
    limit: Option<usize>,
}

impl<T> BoundedVecDeque<T> {
    fn new(limit: Option<usize>) -> Self {
        BoundedVecDeque {
            deque: VecDeque::default(),
            limit,
        }
    }
}

impl<T> BoundedVecDeque<T> {
    pub fn push(&mut self, elem: T) {
        self.deque.push_back(elem);
        if let Some(limit) = self.limit
            && limit < self.deque.len()
        {
            self.deque.pop_front();
        }
    }
}

impl<T> IntoIterator for BoundedVecDeque<T> {
    type Item = T;

    type IntoIter = std::collections::vec_deque::IntoIter<Self::Item>;

    fn into_iter(self) -> Self::IntoIter {
        self.deque.into_iter()
    }
}

#[derive(Debug)]
struct ClientSyncInfo {
    start_pc: u64,
    end_pc: u64,
    insn_sizes: Vec<u64>,
}

impl ClientSyncInfo {
    fn is_jump(&self) -> bool {
        let insn_sum: u64 = self.insn_sizes.iter().sum();
        self.start_pc + insn_sum != self.end_pc
    }
}

pub struct Broker {
    clients: Vec<Client>,
    pub traces: Arc<RwLock<BoundedVecDeque<Vec<TraceData>>>>,
}

impl Broker {
    pub fn create(config: &Config) -> Result<Self> {
        let clients = config
            .qemu
            .clients
            .iter()
            .enumerate()
            .map(|(idx, _)| Client::create(config, idx))
            .collect::<Result<Vec<_>>>()?;

        let trace_limit = if config.testing.max_trace_length < 0 {
            None
        } else {
            Some(config.testing.max_trace_length as usize)
        };

        Ok(Self {
            clients,
            traces: Arc::new(RwLock::new(BoundedVecDeque::new(trace_limit))),
        })
    }

    pub fn run(&mut self, config: &Config) -> Result<Report> {
        debug!(?config, "running broker");

        let diffs = match config.testing.protocol.mode {
            crate::config::ProtocolMode::Lockstep => self.run_lockstep(config),
        }?;

        for client in &mut self.clients {
            client.terminate()?;
        }

        Ok(diffs.into())
    }

    fn run_lockstep(&mut self, config: &Config) -> Result<Vec<DiffEntry>> {
        for (idx, client) in self.clients.iter_mut().enumerate() {
            let client_cfg = config.for_client(idx);
            client.run_n_times(client_cfg.skip_n_instructions, config);
        }

        let mut diffs = vec![];

        let mut stop_after = config.testing.protocol.stop_after_n_instructions;

        match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn | crate::config::ProtocolLayer::TBStrict => {
                while self.any_client_open() {
                    for client in &mut self.clients {
                        if client.is_open {
                            client.run(config);
                        }
                    }

                    if !config.testing.protocol.execute_all_remaining_instructions {
                        if stop_after > 0 {
                            stop_after -= 1;
                        } else {
                            return Ok(diffs);
                        }
                    }

                    self.trace_clients(config)?;
                    diffs.append(&mut self.diff_clients(config));

                    if !diffs.is_empty() {
                        return Ok(diffs);
                    }
                }
            }
            crate::config::ProtocolLayer::TB => {
                while self.any_client_open() {
                    let mut client_sync_infos = vec![];
                    let mut insns_executed_per_client = vec![0; self.clients.len()];

                    for (idx, client) in &mut self.clients.iter_mut().enumerate() {
                        let client_shm = client.shm.clone();
                        while client.is_open {
                            let shm = unsafe { &client_shm.read().shm_tb };
                            let start_pc = shm.tb_info.pc;
                            let insn_sizes = shm
                                .tb_info
                                .insns_info_slice()
                                .iter()
                                .map(|i| i.data.size as u64)
                                .collect::<Vec<_>>();
                            insns_executed_per_client[idx] = insn_sizes.len();

                            if client.run(config) {
                                let end_pc = shm.tb_info.pc;
                                let sync_info = ClientSyncInfo {
                                    start_pc,
                                    end_pc,
                                    insn_sizes,
                                };

                                if sync_info.is_jump() {
                                    debug!(?sync_info, "found jump");
                                    client_sync_infos.push(sync_info);
                                    break;
                                }
                            }
                        }
                    }

                    // Every client reached a jump-instruction - therefore all should be at the same PC
                    // and have executed the same amount of instructions
                    let end_pc_diverged = client_sync_infos
                        .iter()
                        .any(|i| i.end_pc != client_sync_infos[0].end_pc);

                    let insns_executed_diverged = insns_executed_per_client
                        .iter()
                        .any(|i| *i != insns_executed_per_client[0]);

                    if end_pc_diverged || insns_executed_diverged {
                        debug!("client diverged during tb synchronization");

                        // NOTE: maybe return an error here instead?
                        return Ok(diffs);
                    }

                    if !config.testing.protocol.execute_all_remaining_instructions {
                        if stop_after > 0 {
                            stop_after -= 1;
                        } else {
                            return Ok(diffs);
                        }
                    }

                    self.trace_clients(config)?;
                    diffs.append(&mut self.diff_clients(config));

                    if !diffs.is_empty() {
                        return Ok(diffs);
                    }
                }
            }
        }

        Ok(diffs)
    }

    fn any_client_open(&self) -> bool {
        self.clients.iter().any(|c| c.is_open)
    }

    fn diff_clients(&mut self, config: &Config) -> Vec<DiffEntry> {
        for i in 0..self.clients.len() {
            if let Some(j) = (i + 1..self.clients.len()).next() {
                let c1 = &self.clients[i];
                let c2 = &self.clients[j];

                match config.testing.protocol.layer {
                    crate::config::ProtocolLayer::Insn => {
                        let c1insn = unsafe { &c1.shm.read().shm_exec };
                        let c2insn = unsafe { &c2.shm.read().shm_exec };

                        let ctx1 = DiffContextClient::from_insn(c1, c1insn);
                        let ctx2 = DiffContextClient::from_insn(c2, c2insn);

                        return diff_cpus(
                            &c1insn.cpus,
                            c1insn.init_mask,
                            &c2insn.cpus,
                            c2insn.init_mask,
                            config,
                            vec![ctx1, ctx2],
                        );
                    }
                    crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
                        let c1insn = unsafe { &c1.shm.read().shm_tb };
                        let c2insn = unsafe { &c2.shm.read().shm_tb };

                        let ctx1 = DiffContextClient::from_tb(c1, c1insn);
                        let ctx2 = DiffContextClient::from_tb(c2, c2insn);

                        // NOTE: also diff instruction info especially for "tb-strict"
                        return diff_cpus(
                            &c1insn.cpus,
                            c1insn.init_mask,
                            &c2insn.cpus,
                            c2insn.init_mask,
                            config,
                            vec![ctx1, ctx2],
                        );
                    }
                }
            }
        }

        // if less than 2 clients are configured -> no diff will be returned
        Vec::new()
    }

    fn add_trace_entry(&mut self, trace: Vec<TraceData>) -> Result<()> {
        let Ok(mut lock) = self.traces.write() else {
            bail!("rwlock of trace-queue is poisoned");
        };

        lock.push(trace);

        Ok(())
    }

    fn trace_clients(&mut self, config: &Config) -> Result<()> {
        if !config.tracing.enable {
            return Ok(());
        }

        let trace = match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => self
                .clients
                .iter()
                .map(|c| unsafe { c.shm.read().shm_exec.clone() })
                .map(ManuallyDrop::into_inner)
                .map(Box::new)
                .map(TraceData::Exec)
                .collect(),
            crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => self
                .clients
                .iter()
                .map(|c| unsafe { c.shm.read().shm_tb.clone() })
                .map(ManuallyDrop::into_inner)
                .map(Box::new)
                .map(TraceData::TB)
                .collect(),
        };

        self.add_trace_entry(trace)?;
        Ok(())
    }
}
