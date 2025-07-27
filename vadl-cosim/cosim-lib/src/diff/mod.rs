use std::fmt::Debug;

use serde::Serialize;

use crate::{
    config::Config,
    ipc::{
        cstructs::{BrokerSHMExec, BrokerSHMTB, SHMCPU, SHMRegister, TBInsnInfo},
        qemu::Client,
    },
};

#[allow(clippy::module_inception)]
pub mod diff;

#[derive(Debug, Serialize)]
pub struct Report {
    passed: bool,
    diffs: Vec<DiffEntry>,
    diff_context: DiffContext,
}

impl Report {
    pub fn passed() -> Self {
        Report {
            passed: true,
            diffs: Vec::new(),
            diff_context: Vec::new(),
        }
    }

    pub fn failed(diffs: Vec<DiffEntry>, diff_context: DiffContext) -> Self {
        Report {
            passed: false,
            diffs,
            diff_context,
        }
    }
}

#[derive(Debug, Serialize)]
pub struct DiffEntry {
    key: String,
    values: Vec<String>,
    description: String,
}

pub type DiffContext = Vec<DiffContextClient>;

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClient {
    client_id: usize,
    client_name: Option<String>,
    client_run_count: u64,
    before_state: DiffContextClientState,
    error_instruction: DiffContextClientInstructions,
    after_state: DiffContextClientState,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientState {
    pc: u64,
    cpus: Vec<DiffContextClientStateCPU>,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientStateCPU {
    registers: Vec<DiffContextClientStateRegister>,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientStateRegister {
    name: String,
    value: String,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientInstructions(Vec<DiffContextClientInstruction>);

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientInstruction {
    pc: u64,
    hwaddr: String,
    disas: String,
    insn_data: String,
}

pub enum DiffValue {
    Int(i64),
    Str(String),
}

impl DiffEntry {
    pub fn new(
        key: impl Into<String>,
        values: Vec<String>,
        description: impl Into<String>,
    ) -> Self {
        Self {
            key: key.into(),
            values,
            description: description.into(),
        }
    }
}

impl DiffContextClient {
    pub fn new<T: Into<DiffContextClientState>>(
        client_id: usize,
        client_name: Option<String>,
        client_run_count: u64,
        before_state: T,
        error_instruction: DiffContextClientInstructions,
        after_state: T,
    ) -> Self {
        Self {
            client_id,
            client_name,
            client_run_count,
            before_state: before_state.into(),
            error_instruction,
            after_state: after_state.into(),
        }
    }
}

pub fn get_all_clients_instructions(
    clients: &[Client],
    config: &Config,
) -> Vec<DiffContextClientInstructions> {
    clients
        .iter()
        .map(|client| get_client_instructions(client, config))
        .collect()
}

pub fn get_client_instructions(client: &Client, config: &Config) -> DiffContextClientInstructions {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            let exec = &client.shm.get_exec().insn_info;
            let insn = exec.into();
            DiffContextClientInstructions(vec![insn])
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            let tb = client.shm.get_tb();
            let insns = tb
                .tb_info
                .insns_info_slice()
                .iter()
                .map(|insn| insn.into())
                .collect();
            DiffContextClientInstructions(insns)
        }
    }
}

impl From<&TBInsnInfo> for DiffContextClientInstruction {
    fn from(value: &TBInsnInfo) -> Self {
        DiffContextClientInstruction {
            pc: value.pc,
            hwaddr: value.hwaddr.as_str().to_owned(),
            disas: value.disas.as_str().to_owned(),
            insn_data: value.data.buffer_slice_fmt(),
        }
    }
}

/// A wrapper around the SHM-Data. This is primarily used during the diff-context collection.
/// Why: The before-state needs to be saved (cloned) since the data changes when a client is run.
///      While it is possible to directly save the DiffContextClientState, this involves a lot of
///      string-formatting operations which is significantly slower than just copying the data.
///      Therefore the data is first cloned and then later (when a diff is actually found)
///      converted.
/// NOTE: While this is already much better performance-wise, another (more performant) solution
/// might be:
///     1. Use two shared-memory objects instead of one
///     2. Alternate between them both from the broker and the qemu-client side
///     3. The currenlty used object contains the "after" state, while the previous one still
///        contains the "before" state.
///     This way no cloning has to be done and the memory is allocated once at program-start.
///     With the only "downside" being that this would obviously allocated double the amount of
///     memory for the SHM-Data during the whole cosimulation process.
#[allow(clippy::large_enum_variant)]
pub enum BrokerSHMWrapper {
    TB(BrokerSHMTB),
    Exec(BrokerSHMExec),
}

pub fn clone_client_shms(clients: &[Client], config: &Config) -> Vec<BrokerSHMWrapper> {
    clients
        .iter()
        .map(|client| match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => BrokerSHMWrapper::Exec(client.shm.get_exec().clone()),
            crate::config::ProtocolLayer::TB |
            crate::config::ProtocolLayer::TBStrict => BrokerSHMWrapper::TB(client.shm.get_tb().clone()),
        })
        .collect()
}

pub fn get_all_clients_contexts(
    clients: &[Client],
    config: &Config,
) -> Vec<DiffContextClientState> {
    clients
        .iter()
        .map(|client| get_client_context(client, config))
        .collect()
}

pub fn get_client_context_from_wrapper(wrapper: BrokerSHMWrapper, config: &Config) -> DiffContextClientState {
    match wrapper {
        BrokerSHMWrapper::TB(broker_shmtb) => (&broker_shmtb, config).into(),
        BrokerSHMWrapper::Exec(broker_shmexec) => (&broker_shmexec, config).into(),
    }
}

pub fn get_client_context(client: &Client, config: &Config) -> DiffContextClientState {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => (client.shm.get_exec(), config).into(),
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            (client.shm.get_tb(), config).into()
        }
    }
}

impl From<(&BrokerSHMTB, &Config)> for DiffContextClientState {
    fn from((value, config): (&BrokerSHMTB, &Config)) -> Self {
        let cpus = value.cpus.iter().map(|cpu| (cpu, config).into()).collect();

        DiffContextClientState {
            pc: value.tb_info.pc,
            cpus,
        }
    }
}

impl From<(&BrokerSHMExec, &Config)> for DiffContextClientState {
    fn from((value, config): (&BrokerSHMExec, &Config)) -> Self {
        let cpus = value.cpus.iter().map(|cpu| (cpu, config).into()).collect();

        DiffContextClientState {
            pc: value.insn_info.pc,
            cpus,
        }
    }
}

impl From<(&SHMCPU, &Config)> for DiffContextClientStateCPU {
    fn from((value, config): (&SHMCPU, &Config)) -> Self {
        let registers = value
            .registers_slice()
            .iter()
            .map(|r| (r, config).into())
            .collect();

        DiffContextClientStateCPU { registers }
    }
}

impl From<(&SHMRegister, &Config)> for DiffContextClientStateRegister {
    fn from((value, config): (&SHMRegister, &Config)) -> Self {
        DiffContextClientStateRegister {
            name: value.mapped_name(config).to_owned(),
            value: value.data_slice_fmt(),
        }
    }
}
