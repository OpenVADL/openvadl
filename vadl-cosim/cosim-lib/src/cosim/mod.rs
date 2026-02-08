use std::collections::HashSet;
use std::path::Path;
use std::fs;

use color_eyre::{
    Result, Section,
    eyre::{Error, anyhow, bail},
};
use tracing::debug;

#[cfg(feature = "sqlite-tracing")]
use crate::db::{
    CosimRunInfo, DBConnection, finish_cosimulation_run_trace, insert_new_cosimulation_run,
};

use crate::ipc::cstructs::BrokerSHMInsn;
#[cfg(feature = "sqlite-tracing")]
use crate::trace::{
    TraceEntryData, TraceStore, connect, get_client_trace, store_trace, trace_collect,
};

use crate::{
    config::Config,
    diff::{
        DiffContext, DiffContextClient, DiffEntry, Report,
        diff::{diff_cpus, diff_mem_access},
        get_all_clients_contexts_before, get_all_clients_contexts_current,
        get_all_clients_instructions,
    },
    ipc::{
        cstructs::{self, TBInfo, TBInsnInfo},
        qemu::Client,
    },
};

pub struct Broker {
    clients: Vec<Client>,
    #[cfg(feature = "sqlite-tracing")]
    run_info: Option<CosimRunInfo>,
    #[cfg(feature = "sqlite-tracing")]
    trace_store: TraceStore,
    #[cfg(feature = "sqlite-tracing")]
    trace_connection: DBConnection,
}

enum TBSyncResult {
    Success,
    Diverged(DiffEntry),
}

impl Broker {
    #[cfg(not(feature = "sqlite-tracing"))]
    pub fn create(config: &Config) -> Result<Self> {
        let clients = config
            .qemu
            .clients
            .iter()
            .enumerate()
            .map(|(idx, _)| Client::create(config, idx))
            .collect::<Result<Vec<_>>>()?;

        Ok(Self { clients })
    }

    #[cfg(feature = "sqlite-tracing")]
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

    #[allow(unused_variables)]
    pub fn finish(mut self, passed: bool, config: &Config) -> Result<()> {
        #[cfg(feature = "sqlite-tracing")]
        {
            if config.tracing.mode.is_collect() {
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
        }

        for client in &mut self.clients {
            client.terminate()?;
        }

        Ok(())
    }

    pub fn clients(&self) -> &Vec<Client> {
        &self.clients
    }

    fn add_client_logs_to_error(err: Error, client_id: &str, config: &Config) -> Error {
        let base_path = Path::new(&config.logging.dir);
        let stdout_path = base_path.join(format!("client-{client_id}-stdout.txt"));
        let stderr_path = base_path.join(format!("client-{client_id}-stderr.txt"));

        let stdout_content =
            fs::read_to_string(stdout_path).expect("client stdout-file should exist");
        let stderr_content =
            fs::read_to_string(stderr_path).expect("client stderr-file should exist");

        err.note(format!(
            "\nClient stdout:\n{stdout_content}\n\nClient stderr:\n{stderr_content}"
        ))
    }

    fn bcollect<T>(iter: &mut impl Iterator<Item = Result<T>>) -> Result<Vec<T>> {
        let mut errs = vec![];
        let mut res = vec![];

        for elem in iter {
            match elem {
                Ok(elem) => res.push(elem),
                Err(elem) => errs.push(elem),
            }
        }

        if errs.is_empty() {
            Ok(res)
        } else if errs.len() == 1 {
            Err(errs.pop().unwrap())
        } else {
            let mut report = anyhow!("Multiple errors occurred");
            while let Some(err) = errs.pop() {
                report = report.wrap_err(format!("{err:?}"));
            }
            Err(report)
        }
    }

    fn run_lockstep(&mut self, config: &Config) -> Result<Report> {
        // NOTE: maybe move "spawning" the clients into this method
        for (idx, client) in self.clients.iter_mut().enumerate() {
            let client_cfg = config.for_client(idx);
            for _ in 0..client_cfg.skip_n_instructions {
                let _ = client.shm.read_buffer();
                client.shm.end_read_buffer();
            }
            debug!(
                "skipped {} instructions for {:?}",
                client_cfg.skip_n_instructions, client_cfg.name
            );
        }

        let mut stop_after = config.testing.protocol.stop_after_n_instructions;

        self.check_clients_are_initially_synchronized(config)?;

        match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => loop {
                let c1name = self.clients[0].name_or_id();
                let c2name = self.clients[1].name_or_id();
                let mut reads = self.clients.iter_mut().map(|client| {
                    let res = client
                        .shm
                        .read_buffer()
                        .map(|opt| opt.map(|i| i.as_insn()))
                        .map_err(|e| Broker::add_client_logs_to_error(e, &client.id, config));
                    client.run_count += 1;
                    res
                });
                let reads = Broker::bcollect(&mut reads)?;

                let c1insn = reads[0];
                let c2insn = reads[1];

                match (c1insn, c2insn) {
                    // successfully read both clients -> compare
                    (Some(c1insn), Some(c2insn)) => {
                        let diffs = if let Some(cpus1) = c1insn.cpus()
                            && let Some(cpus2) = c2insn.cpus()
                        {
                            let diffs =
                                diff_cpus(cpus1, c1insn.init_mask, cpus2, c2insn.init_mask, config);

                            #[cfg(feature = "sqlite-tracing")]
                            self.trace_clients(config)?;

                            if !config.testing.protocol.execute_all_remaining_instructions {
                                stop_after -= 1;
                                if stop_after == 0 {
                                    break;
                                }
                            }

                            diffs
                        } else if let Some(mem_access_info1) = c1insn.mem_access_info()
                            && let Some(mem_access_info2) = c2insn.mem_access_info()
                        {
                            diff_mem_access(mem_access_info1, mem_access_info2, config)
                        } else {
                            let diff = DiffEntry::new(
                                "missing-memory-access",
                                vec![
                                    Broker::format_insn_for_diff(&c1insn.insn_info),
                                    Broker::format_insn_for_diff(&c2insn.insn_info),
                                ],
                                Broker::format_missing_memory_access_msg(
                                    c1insn,
                                    c2insn,
                                    &c1name,
                                    &c2name,
                                )
                            );
                            vec![diff]
                        };

                        if !diffs.is_empty() {
                            debug!("difference between two instructions found");
                            let ctx = self.build_diff_context(config)?;
                            return Ok(Report::failed(diffs, ctx));
                        }

                        for client in &mut self.clients {
                            client.shm.end_read_buffer();
                        }
                    }

                    // both clients finished at the same time => stop cosimulation
                    (None, None) => break,

                    // one client finished while the other still writes to the buffer, error state!
                    (Some(c1insn), None) => {
                        debug!("one client executes more instructions than the other");
                        let diff = DiffEntry::new(
                            "invalid-execution",
                            vec![Broker::format_insn_for_diff(&c1insn.insn_info)],
                            Broker::format_invalid_execution_client_msg(
                                &self.clients[0],
                                &self.clients[1],
                            ),
                        );
                        let ctx = self.build_diff_context_for_client(config, 0)?;                        
                        return Ok(Report::failed(vec![diff], ctx));
                    }
                    (None, Some(c2insn)) => {
                        debug!("one client executes more instructions than the other");
                        let diff = DiffEntry::new(
                            "invalid-execution",
                            vec![Broker::format_insn_for_diff(&c2insn.insn_info)],
                            Broker::format_invalid_execution_client_msg(
                                &self.clients[1],
                                &self.clients[0],
                            ),
                        );
                        let ctx = self.build_diff_context_for_client(config, 1)?;
                        return Ok(Report::failed(vec![diff], ctx));
                    }
                }
            },
            crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => loop {
                let reads = self
                    .clients
                    .iter_mut()
                    .map(|client| {
                        let res = client.shm.read_buffer().map(|opt| opt.map(|i| i.as_tb()));
                        client.run_count += 1;
                        res
                    })
                    .collect::<Result<Vec<_>>>()?;

                let c1insn = reads[0];
                let c2insn = reads[1];

                match (c1insn, c2insn) {
                    // successfully read both clients -> compare
                    (Some(c1insn), Some(c2insn)) => {
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

                        #[cfg(feature = "sqlite-tracing")]
                        self.trace_clients(config)?;

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
                    }

                    // both clients finished at the same time => stop cosimulation
                    (None, None) => break,

                    // one client finished while the other still writes to the buffer, error state!
                    (Some(c1tb), None) => {
                        let diff = DiffEntry::new(
                            "invalid-execution",
                            vec![Broker::format_tb_for_diff(&c1tb.tb_info)],
                            Broker::format_invalid_execution_client_msg(
                                &self.clients[0],
                                &self.clients[1],
                            ),
                        );
                        let ctx = self.build_diff_context_for_client(config, 0)?;
                        return Ok(Report::failed(vec![diff], ctx));
                    }
                    (None, Some(c2tb)) => {
                        let diff = DiffEntry::new(
                            "invalid-execution",
                            vec![Broker::format_tb_for_diff(&c2tb.tb_info)],
                            Broker::format_invalid_execution_client_msg(
                                &self.clients[1],
                                &self.clients[0],
                            ),
                        );
                        let ctx = self.build_diff_context_for_client(config, 1)?;
                        return Ok(Report::failed(vec![diff], ctx));
                    }
                }
            },
        }

        let ctx = self.build_diff_context(config)?;
        Ok(Report::failed(vec![], ctx))
    }

    #[allow(unused_variables)]
    fn format_missing_memory_access_msg(
        c1insn: &BrokerSHMInsn,
        c2insn: &BrokerSHMInsn,
        c1: &str,
        c2: &str,
    ) -> String {
        let (mem_access_client, insn_exec_client) = if c1insn.mem_access_info().is_some() {
            (c1, c2)
        } else {
            (c2, c1)
        };

        format!(
            "When executing the instruction: {}\n\"{}\" wrote memory-access info to the buffer, while \"{}\" wrote an insn-execution to the buffer",
            Broker::format_insn_for_diff(&c1insn.insn_info),
            mem_access_client,
            insn_exec_client
        )
    }

    fn format_insn_for_diff(insn: &TBInsnInfo) -> String {
        format!(
            "pc={}, size={}, symbol={}, hwaddr={}, disas={}, data={}",
            insn.pc,
            insn.size,
            insn.symbol.as_str(),
            insn.hwaddr.as_str(),
            insn.disas.as_str(),
            insn.data.buffer_slice_fmt(),
        )
    }

    fn format_tb_for_diff(tb: &TBInfo) -> String {
        let insns = tb
            .insns_info_slice()
            .iter()
            .map(Broker::format_insn_for_diff)
            .collect::<Vec<_>>();

        format!("pc={}, insns={:#?}", tb.pc, insns)
    }

    fn format_invalid_execution_client_msg(
        executing_client: &Client,
        halted_client: &Client,
    ) -> String {
        format!(
            "client \"{}\" executed another instruction while client \"{}\" has already finished",
            executing_client.name_or_id(),
            halted_client.name_or_id()
        )
    }

    fn check_clients_are_initially_synchronized(&self, config: &Config) -> Result<()> {
        let start_pcs = self
            .clients
            .iter()
            .map(|c| match config.testing.protocol.layer {
                crate::config::ProtocolLayer::Insn => {
                    c.shm.read_buffer_prev().as_insn().insn_info.pc
                }
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
    #[cfg(feature = "sqlite-tracing")]
    fn trace_clients(&mut self, config: &Config) -> Result<()> {
        use crate::config::TracingMode;
        match config.tracing.mode {
            TracingMode::None => Ok(()),
            TracingMode::Collect => trace_collect(
                &mut self.clients,
                &self.run_info.as_ref().unwrap().client_ids,
                config,
                &mut self.trace_store,
            ),
            TracingMode::Sync => {
                for (idx, client) in self.clients.iter_mut().enumerate() {
                    let broker_data = get_client_trace(client, config)?;
                    let client_id = self.run_info.as_ref().unwrap().client_ids[idx];
                    let trace = TraceEntryData::new(client_id, client.run_count, broker_data);
                    store_trace(trace, &mut self.trace_connection)?;
                }
                Ok(())
            }
        }
    }

    fn build_diff_context_for_client(&mut self, config: &Config, client_idx: usize) -> Result<DiffContext> {
        let mut before_states = get_all_clients_contexts_before(&self.clients[client_idx..=client_idx], config);
        let mut after_states = get_all_clients_contexts_current(&mut self.clients[client_idx..=client_idx], config)?;
        let mut error_instructions = get_all_clients_instructions(&self.clients[client_idx..=client_idx], config);
        let mut diff_context = vec![];

        for client in &self.clients[client_idx..=client_idx] {
            let before_state = before_states.pop_front().unwrap();
            let error_instruction = error_instructions.pop_front().unwrap();
            let after_state = after_states.pop_front().unwrap();

            let client_id = client.id.clone();
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

    fn build_diff_context(&mut self, config: &Config) -> Result<DiffContext> {
        let mut before_states = get_all_clients_contexts_before(&self.clients, config);
        let mut after_states = get_all_clients_contexts_current(&mut self.clients, config)?;
        let mut error_instructions = get_all_clients_instructions(&self.clients, config);
        let mut diff_context = vec![];

        for client in &self.clients {
            let before_state = before_states.pop_front().unwrap();
            let error_instruction = error_instructions.pop_front().unwrap();
            let after_state = after_states.pop_front().unwrap();

            let client_id = client.id.clone();
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
