use std::{fmt::Display, marker::PhantomData, path::PathBuf};

use serde::{
    Deserialize, Deserializer, Serialize,
    de::{Unexpected, Visitor, value::SeqAccessDeserializer},
};

pub const COSIM_LOG_EXTENSION: &str = "cosim-log";

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Config {
    pub qemu: Qemu,
    pub testing: Testing,
    pub logging: Logging,
    pub dev: Dev,
    #[cfg(feature = "sqlite-tracing")]
    pub tracing: Tracing,
}

impl Config {
    pub fn for_client(&self, idx: usize) -> &Client {
        &self.qemu.clients[idx]
    }
}

#[cfg(feature = "sqlite-tracing")]
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

#[cfg(feature = "sqlite-tracing")]
#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum TracingMode {
    #[default]
    None,
    Collect,
    Sync,
}

#[cfg(feature = "sqlite-tracing")]
impl TracingMode {
    pub fn enabled(&self) -> bool {
        *self != TracingMode::None
    }

    pub fn disabled(&self) -> bool {
        *self == TracingMode::None
    }

    pub fn is_collect(&self) -> bool {
        *self == TracingMode::Collect
    }
}

#[cfg(feature = "sqlite-tracing")]
fn default_tracing_file() -> String {
    "trace.sqlite3".into()
}

#[cfg(feature = "sqlite-tracing")]
fn default_tracing_dir() -> String {
    "./trace".into()
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Logging {
    /// The Loglevel
    pub level: String,

    #[serde(default = "default_logging_file")]
    pub file: Option<PathBuf>,

    #[serde(default = "default_logging_dir")]
    pub dir: PathBuf,

    #[serde(default = "default_true")]
    pub enable: bool,

    #[serde(default = "default_false")]
    pub clear_on_rerun: bool,
}

fn default_logging_file() -> Option<PathBuf> {
    Some("cosim.log".into())
}

fn default_logging_dir() -> PathBuf {
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

    pub with_memory_checks: bool,

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
    #[serde(default)]
    pub exit_condition: ExitCondition,
}


#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct ExitCondition {
    pub on_address: Option<u64>, 
    pub on_label: Option<String>,
    #[serde(default)]
    pub on_mem_write: MemWriteExitCondition,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct MemWriteExitCondition {
    pub on_address: Option<u64>,
    pub on_label: Option<String>,
    pub with_constant_value: Option<u128>,
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
type RegHashSet = fnv::FnvHashSet<String>;

#[derive(Debug, Serialize, Deserialize, Clone)]
#[serde(from = "SlicedRegIntermediate")]
pub struct SlicedReg {
    pub name: String,
    pub mask: u64,
    pub shift_right: u64,
}

impl SlicedReg {
    pub fn apply(&self, val: &mut u64) {
        *val &= self.mask;
        *val >>= self.shift_right;
    }
}

impl From<SlicedRegIntermediate> for SlicedReg {
    fn from(value: SlicedRegIntermediate) -> Self {
        let shift_right = match value.shift_right {
            Some(shift_right) => shift_right,
            None => {
                let mut idx = 0;
                while value.mask & (1 << idx) == 0 {
                    idx += 1;
                }
                idx
            }
        };

        Self {
            name: value.name,
            mask: value.mask,
            shift_right,
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct SlicedRegIntermediate {
    pub name: String,
    #[serde(
        deserialize_with = "mask_array_of_u64_or_range",
        default = "default_mask",
        rename(deserialize = "slice")
    )]
    pub mask: u64,
    pub shift_right: Option<u64>,
}

fn default_mask() -> u64 {
    u64::MAX
}

fn mask_array_of_u64_or_range<'de, D>(deserializer: D) -> Result<u64, D::Error>
where
    D: Deserializer<'de>,
{
    struct Data(PhantomData<fn() -> u64>);

    impl<'de> Visitor<'de> for Data {
        type Value = u64;

        fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
            formatter.write_str("wrong")
        }

        fn visit_seq<A>(self, seq: A) -> Result<Self::Value, A::Error>
        where
            A: serde::de::SeqAccess<'de>,
        {
            #[derive(Deserialize)]
            #[serde(untagged)]
            enum SliceVariants {
                Int(u64),
                Array(Vec<u64>),
            }

            type SliceVariantsVec = Vec<SliceVariants>;
            let variants = SliceVariantsVec::deserialize(SeqAccessDeserializer::new(seq))?;

            let mut mask: u64 = 0;
            let mut min_pos = u64::MAX;
            for v in variants {
                match v {
                    SliceVariants::Int(idx) => {
                        min_pos = u64::min(min_pos, idx);
                        set_bit_at(&mut mask, idx)
                    }
                    SliceVariants::Array(range) => match &range[..] {
                        [from, to] => {
                            if to < from {
                                let fixed_range = vec![to, from];
                                let msg = format!(
                                    "a list with exactly two elements, indicating the inclusive range (from..=to) of the bit-slice (in this case the order of the numbers needs to be swapped, e.g. {fixed_range:?})"
                                );
                                return Err(serde::de::Error::invalid_value(
                                    Unexpected::Other(&format!("{range:?}")),
                                    &msg.as_str(),
                                ));
                            }

                            for idx in *from..=*to {
                                min_pos = u64::min(min_pos, idx);
                                set_bit_at(&mut mask, idx)
                            }
                        }
                        _ => {
                            return Err(serde::de::Error::invalid_value(
                                Unexpected::Other(&format!("{range:?}")),
                                &"a list with exactly two elements, indicating the inclusive range (from..=to) of the bit-slice",
                            ));
                        }
                    },
                }
            }

            Ok(mask)
        }
    }

    deserializer.deserialize_any(Data(PhantomData))
}

fn set_bit_at(val: &mut u64, idx: u64) {
    *val |= 1 << idx;
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct SlicedRegEntry {
    pub client1: SlicedReg,
    pub client2: SlicedReg,
}

#[derive(Debug, Serialize, Deserialize, Clone, Default)]
pub struct SlicedRegMap(pub Vec<SlicedRegEntry>);

impl SlicedRegMap {
    pub fn get_mappings_for(&self, name: &str) -> Vec<&SlicedRegEntry> {
        self.0
            .iter()
            .filter(|entry| entry.client1.name == name || entry.client2.name == name)
            .collect()
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Qemu {
    pub plugin: String,
    pub clients: Vec<Client>,
    pub gdb_reg_map: RegHashMap,

    #[serde(default = "SlicedRegMap::default")]
    pub sliced_reg_map: SlicedRegMap,

    #[serde(default = "empty_hashset")]
    pub defined_registers_map: RegHashSet,
    pub ignore_registers: RegHashSet,
    pub ignore_unset_registers: bool,
}

fn empty_hashset() -> RegHashSet {
    RegHashSet::default()
}

impl Qemu {
    pub fn set_defined_registers_map(&mut self) {
        for (k, v) in &self.gdb_reg_map {
            self.defined_registers_map.insert(k.clone());
            self.defined_registers_map.insert(v.clone());
        }

        for entry in &self.sliced_reg_map.0 {
            self.defined_registers_map.insert(entry.client1.name.clone());
            self.defined_registers_map.insert(entry.client2.name.clone());
        }
    }

    pub fn has_equal_endianess(&self) -> bool {
        match &self.clients[..] {
            [] => true,
            [head, tail @ ..] => tail.iter().all(|c| c.endian == head.endian),
        }
    }
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Dev {
    pub dry_run: bool,
}
