use std::{collections::VecDeque, fmt::Debug};

use color_eyre::{eyre::anyhow, Result};
use serde::Serialize;

use crate::{
    config::{self, Config}, ipc::{
        cstructs::{BrokerSHMInsn, BrokerSHMTB, MemAccessInfo, SHMCPU, SHMRegister, TBInsnInfo},
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
    pub client_id: String,
    pub client_name: Option<String>,
    pub client_run_count: u64,
    pub before_state: DiffContextClientState,
    pub error_instruction: DiffContextClientInstructions,
    pub after_state: Option<DiffContextClientState>,
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientState {
    pub pc: u64,
    pub content: DiffContextClientStateContent,
}

#[derive(Debug, Serialize, Clone)]
#[serde(tag = "type", content = "value")]
pub enum DiffContextClientStateContent {
    CPUs(Vec<DiffContextClientStateCPU>),
    Memory(DiffContextClientStateMemory),
}

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClientStateMemory {
    pub vaddr: u64,
    pub size: u8,
    pub data: String,
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
    pub fn new_without_after_state(
        client_id: String,
        client_name: Option<String>,
        client_run_count: u64,
        before_state: DiffContextClientState,
        error_instruction: DiffContextClientInstructions,
    ) -> Self {
        Self {
            client_id,
            client_name,
            client_run_count,
            before_state,
            error_instruction,
            after_state: None,
        }
    }

    pub fn new_with_after_state(
        client_id: String,
        client_name: Option<String>,
        client_run_count: u64,
        before_state: DiffContextClientState,
        error_instruction: DiffContextClientInstructions,
        after_state: DiffContextClientState,
    ) -> Self {
        Self {
            client_id,
            client_name,
            client_run_count,
            before_state,
            error_instruction,
            after_state: Some(after_state),
        }
    }
}

pub fn get_all_clients_instructions(
    clients: &[Client],
    config: &Config,
) -> VecDeque<DiffContextClientInstructions> {
    clients
        .iter()
        .map(|client| get_client_instructions(client, config))
        .collect()
}

pub fn get_client_instructions(client: &Client, config: &Config) -> DiffContextClientInstructions {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            let exec = &client.shm.read_buffer_prev().as_insn().insn_info;
            let insn = exec.into();
            DiffContextClientInstructions(vec![insn])
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            let tb = client.shm.read_buffer_prev().as_tb();
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

pub fn get_all_clients_contexts_before(
    clients: &[Client],
    config: &Config,
) -> VecDeque<DiffContextClientState> {
    clients
        .iter()
        .map(|client| get_client_context_before(config, client))
        .collect()
}

pub fn get_client_context_before(config: &Config, client: &Client) -> DiffContextClientState {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            diffcontext_insn(
                client.shm.read_buffer_prev().as_insn(),
                config,
                config.for_client(client.config_idx),
            )
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            diffcontext_tb(
                client.shm.read_buffer_prev().as_tb(),
                config,
                config.for_client(client.config_idx),
            )
        }
    }
}

pub fn get_all_clients_contexts_current(
    clients: &mut [Client],
    config: &Config,
) -> Result<VecDeque<DiffContextClientState>> {
    clients
        .iter_mut()
        .map(|client| get_client_context_current(client, config))
        .collect()
}

pub fn get_client_context_current(
    client: &mut Client,
    config: &Config,
) -> Result<DiffContextClientState> {
    let client_config = config.for_client(client.config_idx);
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            let insn = client
                .shm
                .read_buffer()?
                .ok_or(anyhow!("expected to be able to read ringbuffer"))?
                .as_insn();

            let ctx = diffcontext_insn(
                insn,
                config,
                client_config,
            );

            Ok(ctx)
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            let tb = client
                .shm
                .read_buffer()?
                .ok_or(anyhow!("expected to be able to read ringbuffer"))?
                .as_tb();

            let ctx = diffcontext_tb(
                tb,
                config,
                client_config,
            );

            Ok(ctx)
        }
    }
}

fn diffcontext_tb(
    tb: &BrokerSHMTB,
    config: &Config,
    client_config: &config::Client
) -> DiffContextClientState {
    let cpus = tb.cpus
        .iter()
        .map(|cpu| diffcontext_cpu(cpu, config, client_config))
        .collect();
    let content = DiffContextClientStateContent::CPUs(cpus);

    DiffContextClientState {
        pc: tb.tb_info.pc,
        content,
    }
}

fn diffcontext_insn(insn: &BrokerSHMInsn, config: &Config, client_config: &config::Client) -> DiffContextClientState {
    let content = match insn.insn_data_type {
        crate::ipc::cstructs::BrokerSHMInsnDataType::InsnExec => {
            let cpus = insn
                .cpus()
                .unwrap()
                .iter()
                .map(|cpu| diffcontext_cpu(cpu, config, client_config))
                .collect();
            DiffContextClientStateContent::CPUs(cpus)
        }
        crate::ipc::cstructs::BrokerSHMInsnDataType::InsnMem => {
            let mem = diffcontext_memory_access(
                insn.mem_access_info().unwrap(),
                config,
                client_config,
            );
            DiffContextClientStateContent::Memory(mem)
        }
    };

    DiffContextClientState {
        pc: insn.insn_info.pc,
        content,
    }
}

fn diffcontext_memory_access(
    mem_access: &MemAccessInfo,
    _config: &Config,
    client_config: &config::Client
) -> DiffContextClientStateMemory {
    DiffContextClientStateMemory {
        vaddr: mem_access.vaddr,
        size: mem_access.size,
        data: mem_access.data_slice_fmt(client_config.endian),
    }
}

fn diffcontext_cpu(
    cpu: &SHMCPU,
    config: &Config,
    client_config: &config::Client,
) -> DiffContextClientStateCPU {
    let registers = cpu
        .registers_slice()
        .iter()
        .map(|reg| diffcontext_register(reg, config, client_config))
        .collect();

    DiffContextClientStateCPU { registers }
}

fn diffcontext_register(
    register: &SHMRegister,
    config: &Config,
    client_config: &config::Client,
) -> DiffContextClientStateRegister {
    DiffContextClientStateRegister {
        name: register.mapped_name(config).to_owned(),
        value: register.data_slice_fmt(client_config.endian),
    }
}
