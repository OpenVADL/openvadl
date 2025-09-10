use std::
    collections::HashSet
;

use anyhow::{Result, bail};
use rusqlite::Connection;
use tracing::debug;

use crate::{
    config::{Config, TracingMode},
    db::{finish_cosimulation_run_trace, insert_new_cosimulation_run, CosimRunInfo},
    diff::{diff::diff_cpus, get_all_clients_contexts_before, get_all_clients_contexts_current, get_all_clients_instructions, DiffContext, DiffContextClient, DiffEntry, Report},
    ipc::{cstructs, qemu::Client},
    trace::{connect, store_trace, trace_collect, TraceStore},
};

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
            client.terminate().unwrap();
        }

        Ok(())
    }

    fn run_lockstep(&mut self, config: &Config) -> Result<Report> {
        // NOTE: maybe move "spawning" the clients into this method
        for (idx, client) in self.clients
            .iter_mut()
            .enumerate() {
            let client_cfg = config.for_client(idx);
            for _ in 0..client_cfg.skip_n_instructions {
                let _ = client.shm.read_buffer();
                client.shm.end_read_buffer();
            }
            debug!("skipped {} instructions for {:?}", client_cfg.skip_n_instructions, client_cfg.name);
        }

        let mut stop_after = config.testing.protocol.stop_after_n_instructions;

        self.check_clients_are_initially_synchronized(config)?;

        match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => loop {
                let reads = self
                    .clients
                    .iter_mut()
                    .map(|client| client.shm.read_buffer().map(|i| i.as_insn()))
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


                if !diffs.is_empty() {
                    let ctx = self.build_diff_context(config)?;
                    return Ok(Report::failed(diffs, ctx));
                }

                for client in &mut self.clients {
                    client.shm.end_read_buffer();
                }

                if !config.testing.protocol.execute_all_remaining_instructions {
                    stop_after -= 1;
                    if stop_after == 0 {
                        break;
                    }
                }
            },
            crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => loop {
                let reads = self
                    .clients
                    .iter_mut()
                    .map(|client| client.shm.read_buffer().map(|i| i.as_tb()))
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
                    let ctx = self.build_diff_context(config)?;
                    return Ok(Report::failed(diffs, ctx));
                }

                for client in &mut self.clients {
                    client.shm.end_read_buffer();
                }

                if !config.testing.protocol.execute_all_remaining_instructions {
                    stop_after -= 1;
                    if stop_after == 0 {
                        break;
                    }
                }
            },
        }

        Ok(Report::passed())
    }

    fn check_clients_are_initially_synchronized(&self, config: &Config) -> Result<()> {
        let start_pcs = self
            .clients
            .iter()
            .map(|c| match config.testing.protocol.layer {
                crate::config::ProtocolLayer::Insn => c.shm.read_buffer_prev().as_insn().insn_info.pc,
                crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
                    c.shm.read_buffer_prev().as_tb().tb_info.pc
                }
            })
            .collect::<Vec<_>>();

        if start_pcs.iter().any(|pc| *pc != start_pcs[0]) {
            bail!("clients started sync from different start-pcs: {start_pcs:?}")
        }

        Ok(())
    }

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

    fn build_diff_context(&mut self, config: &Config) -> anyhow::Result<DiffContext> {
        let mut before_states = get_all_clients_contexts_before(&self.clients, config);
        let mut after_states = get_all_clients_contexts_current(&mut self.clients, config)?;
        let mut error_instructions = get_all_clients_instructions(&self.clients, config);
        let mut diff_context = vec![];

        for client in &self.clients {
            let before_state = before_states.pop().unwrap();
            let error_instruction = error_instructions.pop().unwrap();
            let after_state = after_states.pop().unwrap();

            let client_id = client.id;
            let client_name = client.name.clone();
            let client_run_count = client.run_count;

            diff_context.push(DiffContextClient::new(
                client_id,
                client_name,
                client_run_count,
                before_state,
                error_instruction,
                after_state,
            ));
        }

        Ok(diff_context)
    }
}
