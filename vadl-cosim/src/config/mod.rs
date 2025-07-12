use std::{collections::HashMap, fmt::Display};

use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Config {
    pub qemu: Qemu,
    pub testing: Testing,
    pub logging: Logging,
    pub dev: Dev,
    pub tracing: Tracing,
}

impl Config {
    pub fn for_client(&self, idx: usize) -> &Client {
        &self.qemu.clients[idx]
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Tracing {
    #[serde(default = "TracingMode::default")]
    pub mode: TracingMode,

    #[serde(default = "default_tracing_dir")]
    pub dir: String,

    #[serde(default = "default_tracing_file")]
    pub file: String,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum TracingMode {
    #[default]
    None,
    Collect,
    Threaded,
    Sync,
}

fn default_tracing_file() -> String {
    "trace.sqlite3".into()
}

fn default_tracing_dir() -> String {
    "./trace".into()
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Logging {
    /// The Loglevel
    pub level: String,

    #[serde(default = "default_logging_file")]
    pub file: String,

    #[serde(default = "default_logging_dir")]
    pub dir: String,

    #[serde(default = "default_true")]
    pub enable: bool,

    #[serde(default = "default_false")]
    pub clear_on_rerun: bool,
}

fn default_logging_file() -> String {
    "cosim.json".into()
}

fn default_logging_dir() -> String {
    "./logs".into()
}

fn default_true() -> bool {
    true
}
fn default_false() -> bool {
    false
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct Out {
    #[serde(default = "default_out_dir")]
    pub dir: String,

    #[serde(default = "OutFormat::default")]
    pub format: OutFormat,
}

fn default_out_dir() -> String {
    "./result".into()
}

#[derive(Serialize, Deserialize, Clone, Debug, Default)]
#[serde(rename_all = "lowercase")]
pub enum OutFormat {
    #[default]
    Json,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Protocol {
    pub mode: ProtocolMode,

    pub layer: ProtocolLayer,

    pub execute_all_remaining_instructions: bool,

    pub stop_after_n_instructions: u32,

    #[serde(default = "Out::default")]
    pub out: Out,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "lowercase")]
pub enum ProtocolMode {
    Lockstep,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "lowercase")]
pub enum ProtocolLayer {
    Insn,
    TB,
    #[serde(rename = "tb-strict")]
    TBStrict,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Testing {
    pub test_exec: String,

    pub protocol: Protocol,

    pub max_trace_length: i32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ClientGDB {
    pub enable: bool,

    pub remote_target: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Client {
    pub exec: String,

    pub pass_test_exec_to: TestExecDestination,

    pub additional_args: Vec<String>,

    pub gdb: ClientGDB,

    pub skip_n_instructions: u32,

    pub name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum TestExecDestination {
    Bios,
    Kernel,
}

impl Display for TestExecDestination {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let res = match self {
            TestExecDestination::Bios => "bios",
            TestExecDestination::Kernel => "kernel",
        };
        write!(f, "{res}")
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Qemu {
    pub plugin: String,
    pub clients: Vec<Client>,
    pub gdb_reg_map: HashMap<String, String>,

    #[serde(default = "empty_hashmap")]
    pub gdb_reg_map_inverse: HashMap<String, String>,
    pub ignore_registers: Vec<String>,
    pub ignore_unset_registers: bool,
}

fn empty_hashmap() -> HashMap<String, String> {
    HashMap::new()
}

impl Qemu {
    pub fn set_inverse_reg_map(&mut self) {
        self.gdb_reg_map_inverse = self.gdb_reg_map
            .iter()
           .map(|(k, v)| (v.clone(), k.clone()))
            .collect();
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Dev {
    pub dry_run: bool,
}
