use std::{
    collections::HashSet,
    thread::{self, JoinHandle}, time::Duration,
};

use anyhow::{Result, bail};
use rusqlite::Connection;
use tracing::debug;

use crate::{
    config::{Config, TracingMode},
    db::{CosimRunInfo, finish_cosimulation_run_trace, insert_new_cosimulation_run},
    diff::{DiffContext, DiffEntry, Report, diff::diff_cpus},
    ipc::{cstructs, qemu::Client},
    trace::{TraceStore, connect, store_trace, trace_collect},
};

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
    run_info: Option<CosimRunInfo>,
    trace_store: TraceStore,
    trace_connection: Connection,
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

        let mut trace_connection = connect(config)?;

        if config.tracing.mode.enabled() {
            let run_info = insert_new_cosimulation_run(&mut trace_connection, &clients)?;
            Ok(Self {
                clients,
                run_info: Some(run_info),
                trace_store: TraceStore::new(),
                trace_connection,
            })
        } else {
            Ok(Self {
                clients,
                run_info: None,
                trace_store: TraceStore::new(),
                trace_connection,
            })
        }
    }

    pub fn run(&mut self, config: &Config) -> Result<Report> {
        debug!(?config, "running broker");

        match config.testing.protocol.mode {
            crate::config::ProtocolMode::Lockstep => self.run_lockstep(config),
        }
    }

    pub fn finish(mut self, passed: bool, config: &Config) -> Result<()> {
        if config.tracing.mode == crate::config::TracingMode::Collect {
            for entry in self.trace_store {
                store_trace(entry, &mut self.trace_connection)?;
            }
        }

        if config.tracing.mode.enabled() {
            finish_cosimulation_run_trace(
                &mut self.trace_connection,
                self.run_info.unwrap(),
                passed,
            )?;
        }

        for client in &mut self.clients {
            client.terminate()?;
        }

        Ok(())
    }

    fn run_lockstep(&mut self, config: &Config) -> Result<Report> {
        // NOTE: maybe move "spawning" the clients into this method

        // TODO: skipping is reading n times from the ring-buffer
        for (idx, client) in self.clients
            .iter_mut()
            .enumerate() {
            let client_cfg = config.for_client(idx);
            for _ in 0..client_cfg.skip_n_instructions {
                let _ = client.shm.get_insn();
                client.shm.get_mut().end_read();
            }
            debug!("skipped {} instructions for {:?}", client_cfg.skip_n_instructions, client_cfg.name);
        }

        let mut stop_after = config.testing.protocol.stop_after_n_instructions;

        // TODO: inital check is a tb_info diff
        // self.check_clients_are_initially_synchronized(config)?;

        // TODO: exit condition
        match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => loop {
                // thread::sleep(Duration::from_secs(1));
                let reads = self
                    .clients
                    .iter_mut()
                    .map(|client| client.shm.get_insn())
                    .collect::<Result<Vec<_>>>()?;


                let c1insn = reads[0];
                let c2insn = reads[1];

                let diffs = diff_cpus(
                    &c1insn.cpus,
                    c1insn.init_mask,
                    &c2insn.cpus,
                    c2insn.init_mask,
                    config,
                );

                for client in &mut self.clients {
                    client.shm.get_mut().end_read();
                }

                if !diffs.is_empty() {
                    return Ok(Report::failed(diffs, self.build_diff_context(config)));
                }
                stop_after -= 1;
                if stop_after == 0 {
                    break;
                }
            },
            crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => loop {
                let reads = self
                    .clients
                    .iter_mut()
                    .map(|client| client.shm.get_tb())
                    .collect::<Result<Vec<_>>>()?;

                let c1insn = reads[0];
                let c2insn = reads[1];

                if let TBSyncResult::Diverged(diff_entry) =
                    Self::check_if_clients_are_synchronized(&[c1insn, c2insn])
                {
                    debug!("client diverged during tb synchronization");
                    return Ok(Report::failed(vec![diff_entry], vec![]));
                }

                let diffs = diff_cpus(
                    &c1insn.cpus,
                    c1insn.init_mask,
                    &c2insn.cpus,
                    c2insn.init_mask,
                    config,
                );

                if !diffs.is_empty() {
                    return Ok(Report::failed(diffs, self.build_diff_context(config)));
                }

                stop_after -= 1;
                if stop_after == 0 {
                    break;
                }
            },
        }

        Ok(Report::passed())
    }

    fn any_client_open(&self) -> bool {
        self.clients.iter().any(|c| c.is_open)
    }

    // fn check_clients_are_initially_synchronized(&self, config: &Config) -> Result<()> {
    //     let start_pcs = self
    //         .clients
    //         .iter()
    //         .map(|c| match config.testing.protocol.layer {
    //             crate::config::ProtocolLayer::Insn => c.shm.current().get_insn().insn_info.pc,
    //             crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
    //                 c.shm.current().get_tb().tb_info.pc
    //             }
    //         })
    //         .collect::<Vec<_>>();
    //
    //     if start_pcs.iter().any(|pc| *pc != start_pcs[0]) {
    //         bail!("clients started sync from different start-pcs: {start_pcs:?}")
    //     }
    //
    //     Ok(())
    // }

    fn check_if_clients_are_synchronized(data: &[&cstructs::BrokerSHMTB]) -> TBSyncResult {
        // Calling this function assumes that every client reached a jump-instruction -
        // therefore all should be at the same PC
        // and have executed the same amount of instructions
        let unique_end_pcs = data
            .iter()
            .map(|b| {
                b.tb_info
                    .insns_info_slice()
                    .last()
                    .expect("no insns in tbinfo")
            })
            .map(|insn| insn.pc)
            .collect::<HashSet<_>>();

        if unique_end_pcs.len() != 1 {
            let all_end_pcs = data
                .iter()
                .map(|b| {
                    b.tb_info
                        .insns_info_slice()
                        .last()
                        .expect("no insns in tbinfo")
                })
                .map(|insn| insn.pc.to_string())
                .collect();

            return TBSyncResult::Diverged(DiffEntry::new(
                "tb_info",
                all_end_pcs,
                "clients reached a different end-pc at the end of tb-synchronization",
            ));
        }

        let unique_number_of_executed_insns = data
            .iter()
            .map(|b| b.tb_info.insns_info_size)
            .collect::<HashSet<_>>();

        if unique_number_of_executed_insns.len() != 1 {
            let all_instr_counts = data
                .iter()
                .map(|b| b.tb_info.insns_info_size.to_string())
                .collect();

            return TBSyncResult::Diverged(DiffEntry::new(
                "tb_info.insns_info_size",
                all_instr_counts,
                "clients reached the same end-pc, but executed a different number of instructions",
            ));
        }

        TBSyncResult::Success
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
                trace_collect(
                    &self.clients,
                    &self.run_info.as_ref().unwrap().client_ids,
                    config,
                    &mut self.trace_store,
                );
                Ok(())
            }
            TracingMode::Sync => {
                for (idx, client) in self.clients.iter().enumerate() {
                    // let broker_data = get_client_trace(client, config);
                    // let client_id = self.run_info.as_ref().unwrap().client_ids[idx];
                    // let trace = TraceEntryData::new(client_id, client.run_count, broker_data);
                    // store_trace(trace, &mut self.trace_connection)?;
                }
                Ok(())
            }
        }
    }

    fn build_diff_context(&self, config: &Config) -> DiffContext {
        return vec![];
        // let mut before_states = get_all_clients_contexts_before(&self.clients, config);
        // let mut after_states = get_all_clients_contexts_current(&self.clients, config);
        // let mut error_instructions = get_all_clients_instructions(&self.clients, config);
        // let mut diff_context = vec![];
        //
        // for client in &self.clients {
        //     let before_state = before_states.pop().unwrap();
        //     let error_instruction = error_instructions.pop().unwrap();
        //     let after_state = after_states.pop().unwrap();
        //
        //     let client_id = client.id;
        //     let client_name = client.name.clone();
        //     let client_run_count = client.run_count;
        //
        //     diff_context.push(DiffContextClient::new(
        //         client_id,
        //         client_name,
        //         client_run_count,
        //         before_state,
        //         error_instruction,
        //         after_state,
        //     ));
        // }
        //
        // diff_context
    }
}
