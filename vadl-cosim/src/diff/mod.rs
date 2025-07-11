use std::fmt::Debug;

use serde::Serialize;

use crate::ipc::{cstructs::{BrokerSHMExec, BrokerSHMTB}, qemu::Client};

pub mod diff;

#[derive(Debug, Serialize)]
pub struct Report {
    passed: bool,
    diffs: Vec<DiffEntry>,
}

impl Report {
    pub fn passed() -> Self {
        Report {
            passed: true,
            diffs: Vec::new(),
        }
    }

    pub fn failed(diffs: Vec<DiffEntry>) -> Self {
        Report {
            passed: false,
            diffs,
        }
    }
}

impl From<Vec<DiffEntry>> for Report {
    fn from(diffs: Vec<DiffEntry>) -> Self {
        if diffs.is_empty() {
            Self::passed()
        } else {
            Self::failed(diffs)
        }
    }
}

#[derive(Debug, Serialize)]
pub struct DiffEntry {
    key: String,
    values: Vec<String>,
    description: String,
    context: DiffContext,
} 

pub type DiffContext = Vec<DiffContextClient>;

#[derive(Debug, Serialize, Clone)]
pub struct DiffContextClient {
    client_id: usize,
    client_name: Option<String>,
    pc: u64,
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
        context: DiffContext,
    ) -> Self {
        Self {
            key: key.into(),
            values,
            description: description.into(),
            context,
        }
    }
}

impl DiffContextClient {
    pub fn new(client_id: usize, client_name: Option<String>, pc: u64) -> Self {
        Self { client_id, client_name, pc }
    }

    pub fn from_tb(client: &Client, shm: &BrokerSHMTB) -> Self {
        Self::new(client.id, client.name.clone(), shm.tb_info.pc)
    }

    pub fn from_insn(client: &Client, shm: &BrokerSHMExec) -> Self {
        Self::new(client.id, client.name.clone(), shm.insn_info.pc)
    }
}
