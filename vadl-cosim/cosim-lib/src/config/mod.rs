use std::fmt::Display;

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

    #[serde(default = "default_true")]
    pub clear_on_rerun: bool,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum TracingMode {
    #[default]
    None,
    Collect,
    Sync,
}

impl TracingMode {
    pub fn enabled(&self) -> bool {
        *self != TracingMode::None
    }

    pub fn disabled(&self) -> bool {
        *self == TracingMode::None
    }
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
    #[serde(default = "OutVerbosity::default")]
    pub verbosity: OutVerbosity,

    pub file: Option<String>,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default)]
#[serde(rename_all = "lowercase")]
pub enum OutVerbosity {
    #[default]
    Full,
    Short,
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
    pub protocol: Protocol,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ClientGDB {
    pub enable: bool,
    #[serde(default = "GDBTargetType::default")]
    pub target_type: GDBTargetType,
    #[serde(default = "default_clientgdb_remote_target")]
    pub remote_target: String,
}

fn default_clientgdb_remote_target() -> String {
    "".into()
}

#[derive(Serialize, Deserialize, Clone, Debug, Default)]
#[serde(rename_all = "lowercase")]
pub enum GDBTargetType {
    #[default]
    Chardev,
    Port,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Client {
    pub exec: String,

    pub test_exec: String,

    pub pass_test_exec_to: TestExecDestination,

    pub additional_args: Vec<String>,

    pub gdb: ClientGDB,

    pub skip_n_instructions: u32,

    pub name: Option<String>,

    #[serde(default = "Endian::default")]
    pub endian: Endian,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum Endian {
    #[default]
    Big,
    Little,
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

// HashMap and HashSet lookups are measurably faster for small key-sizes.
// This makes it usable for a register-map where the keys are register-names which are most of the
// time only a few characters long.
// For reference see: https://cglab.ca/~abeinges/blah/hash-rs/
type RegHashMap = fnv::FnvHashMap<String, String>;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Qemu {
    pub plugin: String,
    pub clients: Vec<Client>,
    pub gdb_reg_map: RegHashMap,

    #[serde(default = "empty_hashmap")]
    pub gdb_reg_map_inverse: RegHashMap,
    pub ignore_registers: fnv::FnvHashSet<String>,
    pub ignore_unset_registers: bool,
}

fn empty_hashmap() -> RegHashMap {
    RegHashMap::default()
}

impl Qemu {
    pub fn set_inverse_reg_map(&mut self) {
        self.gdb_reg_map_inverse = self
            .gdb_reg_map
            .iter()
            .map(|(k, v)| (v.clone(), k.clone()))
            .collect();
    }

    pub fn has_equal_endianess(&self) -> bool {
        match &self.clients[..] {
            [] => true,
            [head, tail @ ..] => tail.iter().all(|c| c.endian == head.endian)
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Dev {
    pub dry_run: bool,
}
