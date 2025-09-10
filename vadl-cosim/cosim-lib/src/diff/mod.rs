use std::fmt::Debug;

use serde::Serialize;

use crate::{
    config::Config,
    ipc::{
        cstructs::{BrokerSHMInsn, BrokerSHMTB, SHMCPU, SHMRegister, TBInsnInfo},
        qemu::Client,
    },
};

#[allow(clippy::module_inception)]
pub mod diff;

#[derive(Debug, Serialize)]
pub struct Report {
    pub passed: bool,
    pub diffs: Vec<DiffEntry>,
    pub diff_context: DiffContext,
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

    pub fn without_context(self) -> Self {
        Report {
            passed: self.passed,
            diffs: self.diffs,
            diff_context: vec![],
        }
    }
}

#[derive(Debug, Serialize)]
pub struct DiffEntry {
    pub key: String,
    pub values: Vec<String>,
    pub description: String,
}

pub type DiffContext = Vec<DiffContextClient>;

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClient {
    pub client_id: usize,
    pub client_name: Option<String>,
    pub client_run_count: u64,
    pub before_state: DiffContextClientState,
    pub error_instruction: DiffContextClientInstructions,
    pub after_state: DiffContextClientState,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientState {
    pub pc: u64,
    pub cpus: Vec<DiffContextClientStateCPU>,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientStateCPU {
    pub registers: Vec<DiffContextClientStateRegister>,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientStateRegister {
    pub name: String,
    pub value: String,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientInstructions(pub Vec<DiffContextClientInstruction>);

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientInstruction {
    pub pc: u64,
    pub hwaddr: String,
    pub disas: String,
    pub insn_data: String,
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
    DiffContextClientInstructions(vec![])
    // match config.testing.protocol.layer {
    //     crate::config::ProtocolLayer::Insn => {
    //         let exec = &client.shm.current().get_insn().insn_info;
    //         let insn = exec.into();
    //         DiffContextClientInstructions(vec![insn])
    //     }
    //     crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
    //         let tb = client.shm.current().get_tb();
    //         let insns = tb
    //             .tb_info
    //             .insns_info_slice()
    //             .iter()
    //             .map(|insn| insn.into())
    //             .collect();
    //         DiffContextClientInstructions(insns)
    //     }
    // }
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

pub fn get_all_clients_contexts_before(
    clients: &[Client],
    config: &Config,
) -> Vec<DiffContextClientState> {
    clients
        .iter()
        .map(|client| get_client_context_before(client, config))
        .collect()
}

pub fn get_client_context_before(client: &Client, config: &Config) -> DiffContextClientState {
    DiffContextClientState { pc: 1234, cpus: vec![] }
    // match config.testing.protocol.layer {
    //     crate::config::ProtocolLayer::Insn => (client.shm.previous().get_insn(), config).into(),
    //     crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
    //         (client.shm.previous().get_tb(), config).into()
    //     }
    // }
}

pub fn get_all_clients_contexts_current(
    clients: &[Client],
    config: &Config,
) -> Vec<DiffContextClientState> {
    clients
        .iter()
        .map(|client| get_client_context_current(client, config))
        .collect()
}

pub fn get_client_context_current(client: &Client, config: &Config) -> DiffContextClientState {
    DiffContextClientState { pc: 1234, cpus: vec![] }
    // match config.testing.protocol.layer {
    //     crate::config::ProtocolLayer::Insn => (client.shm.current().get_insn(), config).into(),
    //     crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
    //         (client.shm.current().get_tb(), config).into()
    //     }
    // }
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

impl From<(&BrokerSHMInsn, &Config)> for DiffContextClientState {
    fn from((value, config): (&BrokerSHMInsn, &Config)) -> Self {
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
