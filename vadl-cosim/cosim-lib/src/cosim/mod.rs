use anyhow::{bail, Result};
use rusqlite::Connection;
use tracing::debug;

use crate::{
    config::{Config, TracingMode},
    diff::{diff::diff_cpus, DiffContextClient, DiffEntry, Report},
    ipc::qemu::Client,
    trace::{
        connect, db::{insert_broker_shm_exec, insert_broker_shm_tb}, trace_collect, trace_sync, trace_threaded, TraceStore
    },
};

#[derive(Debug)]
struct ClientSyncInfo {
    start_pc: u64,
    end_pc: u64,
    insn_sizes: Vec<u64>,
    client_id: usize,
}

impl ClientSyncInfo {
    fn is_jump(&self) -> bool {
        let insn_sum: u64 = self.insn_sizes.iter().sum();
        self.start_pc + insn_sum != self.end_pc
    }
}

pub struct Broker {
    clients: Vec<Client>,
    pub trace_store: TraceStore,
}

enum TBSyncResult {
    Success,
    Diverged(DiffEntry),
}

pub type DBConnection = Connection;

impl Broker {
    pub fn create(config: &Config) -> Result<Self> {
        let clients = config
            .qemu
            .clients
            .iter()
            .enumerate()
            .map(|(idx, _)| Client::create(config, idx))
            .collect::<Result<Vec<_>>>()?;

        Ok(Self {
            clients,
            trace_store: TraceStore::new(),
        })
    }

    pub fn run(&mut self, config: &Config) -> Result<Report> {
        debug!(?config, "running broker");

        let diffs = match config.testing.protocol.mode {
            crate::config::ProtocolMode::Lockstep => self.run_lockstep(config),
        }?;

        Ok(diffs.into())
    }

    pub fn finish(mut self, config: &Config) -> Result<()> {
        if config.tracing.mode == crate::config::TracingMode::Collect {
            for entry in self.trace_store {
                let c = config.clone();
                rayon::spawn(move || {
                    let conn = connect(&c).unwrap();
                    match entry {
                        crate::trace::TraceData::TB(broker_shmtb) => {
                            let _ = insert_broker_shm_tb(&conn, &broker_shmtb);
                        }
                        crate::trace::TraceData::Exec(broker_shmexec) => {
                            let _ = insert_broker_shm_exec(&conn, &broker_shmexec);
                        }
                    }
                });
            }
        }

        for client in &mut self.clients {
            client.terminate()?;
        }

        Ok(())
    }

    fn run_lockstep(&mut self, config: &Config) -> Result<Vec<DiffEntry>> {
        for (idx, client) in self.clients.iter_mut().enumerate() {
            let client_cfg = config.for_client(idx);
            client.run_n_times(client_cfg.skip_n_instructions, config);
        }

        let mut diffs = vec![];
        let mut stop_after = config.testing.protocol.stop_after_n_instructions;

        self.check_clients_are_initially_synchronized(config)?;

        while self.any_client_open() {
            match config.testing.protocol.layer {
                crate::config::ProtocolLayer::Insn | crate::config::ProtocolLayer::TBStrict => {
                    for client in &mut self.clients {
                        if client.is_open {
                            client.run(config);
                        }
                    }
                }
                crate::config::ProtocolLayer::TB => {
                    if let TBSyncResult::Diverged(diff_entry) = self.tb_sync_clients(config) {
                        debug!("client diverged during tb synchronization");
                        diffs.push(diff_entry);
                        return Ok(diffs);
                    }
                }
            };

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

        Ok(diffs)
    }

    fn any_client_open(&self) -> bool {
        self.clients.iter().any(|c| c.is_open)
    }

    fn check_clients_are_initially_synchronized(&self, config: &Config) -> Result<()> {
        let start_pcs = self
            .clients
            .iter()
            .map(|c| {
                match config.testing.protocol.layer {
                    crate::config::ProtocolLayer::Insn => unsafe { &c.shm.get().data.shm_exec }.insn_info.pc,
                    crate::config::ProtocolLayer::TB |
                    crate::config::ProtocolLayer::TBStrict => unsafe { &c.shm.get().data.shm_tb }.tb_info.pc,
                }
            })
            .collect::<Vec<_>>();

        if start_pcs.iter().any(|pc| *pc != start_pcs[0]) {
            bail!("clients started sync from different start-pcs: {start_pcs:?}")
        }

        Ok(())
    }

    fn tb_sync_clients(&mut self, config: &Config) -> TBSyncResult {
        let mut client_sync_infos = vec![];
        let mut insns_executed_per_client = vec![0; self.clients.len()];

        for (idx, client) in &mut self.clients.iter_mut().enumerate() {
            let client_shm = client.shm.clone();
            while client.is_open {
                let shm = unsafe { &client_shm.get().data.shm_tb };
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
                        client_id: client.id,
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

        if end_pc_diverged {
            let end_pcs = client_sync_infos
                .iter()
                .map(|i| i.end_pc.to_string())
                .collect();

            let diff_contexts = client_sync_infos
                .iter()
                .map(|i| (&self.clients[i.client_id], i.end_pc))
                .map(|(c, end_pc)| {
                    DiffContextClient::new(c.id, c.name.clone(), c.run_count, end_pc)
                })
                .collect();

            let diff = DiffEntry::new(
                "tb_info",
                end_pcs,
                "clients reached a different end-pc at the end of tb-synchronization",
                diff_contexts,
            );
            return TBSyncResult::Diverged(diff);
        }

        let insns_executed_diverged = insns_executed_per_client
            .iter()
            .any(|i| *i != insns_executed_per_client[0]);

        if insns_executed_diverged {
            let instr_counts = insns_executed_per_client
                .iter()
                .map(|i| i.to_string())
                .collect();

            let diff_contexts = client_sync_infos
                .iter()
                .map(|i| (&self.clients[i.client_id], i.end_pc))
                .map(|(c, end_pc)| {
                    DiffContextClient::new(c.id, c.name.clone(), c.run_count, end_pc)
                })
                .collect();

            let diff = DiffEntry::new(
                "tb_info.insns_info_size",
                instr_counts,
                "clients reached the same end-pc, but executed a different number of instructions",
                diff_contexts,
            );
            return TBSyncResult::Diverged(diff);
        }

        TBSyncResult::Success
    }

    fn diff_clients(&mut self, config: &Config) -> Vec<DiffEntry> {
        for i in 0..self.clients.len() {
            if let Some(j) = (i + 1..self.clients.len()).next() {
                let c1 = &self.clients[i];
                let c2 = &self.clients[j];

                match config.testing.protocol.layer {
                    crate::config::ProtocolLayer::Insn => {
                        let c1insn = unsafe { &c1.shm.get().data.shm_exec };
                        let c2insn = unsafe { &c2.shm.get().data.shm_exec };

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
                        let c1insn = unsafe { &c1.shm.get().data.shm_tb };
                        let c2insn = unsafe { &c2.shm.get().data.shm_tb };

                        let ctx1 = DiffContextClient::from_tb(c1, c1insn);
                        let ctx2 = DiffContextClient::from_tb(c2, c2insn);

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

    /// Copies (for each client) the current state of the shared memory and spawns a task which
    /// stores the data in a database.
    //
    /// The trace-data is guaranteed (assuming no db-error occurs) to be stored in the database but it is not
    /// blocking since this would drastically reduce cosimulation performance.
    ///
    /// The trace-data is guaranteed to be fully available once the process exits.
    ///
    /// NOTE: The copy of the shared memory is necessary since no lock is placed on it (which would
    /// basically transform the function into a blocking function).
    fn trace_clients(&mut self, config: &Config) -> Result<()> {
        match config.tracing.mode {
            TracingMode::None => Ok(()),
            TracingMode::Collect => {
                trace_collect(&self.clients, config, &mut self.trace_store);
                Ok(())
            }
            TracingMode::Threaded => trace_threaded(&self.clients, config),
            TracingMode::Sync => trace_sync(&self.clients, config),
        }
    }
}
