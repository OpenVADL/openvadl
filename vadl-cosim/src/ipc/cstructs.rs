use std::fmt::Display;

use serde::{ser::SerializeStruct, Serialize};

use crate::config::Config;

pub const SHMSTRING_MAX_LEN: usize = 256;
pub const TBINSNINFO_ENTRIES: usize = 64;
pub const MAX_REGISTER_NAME_SIZE: usize = 64;
pub const MAX_REGISTER_DATA_SIZE: usize = 256;
pub const MAX_CPU_REGISTERS: usize = 256;
pub const MAX_CPU_COUNT: usize = 1;
pub const MAX_INSN_DATA_SIZE: usize = 64;

#[repr(C)]
#[derive(Debug, Clone)]
pub struct SHMString {
    pub len: usize,
    value: [u8; SHMSTRING_MAX_LEN],
}

impl Serialize for SHMString {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        serializer.serialize_str(self.as_str())
    }
}

impl SHMString {
    pub fn as_str(&self) -> &str {
        std::str::from_utf8(&self.value[..self.len]).expect("valid utf8 sequence in SHMString")
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct SHMRegister {
    pub size: i32,
    data: [u8; MAX_REGISTER_DATA_SIZE],
    pub name: SHMString,
}

impl SHMRegister {
    pub fn data_slice(&self) -> &[u8] {
        &self.data[..self.size as usize]
    }

    pub fn data_slice_fmt(&self) -> String {
        format!("{:02X?}", self.data_slice())
    }

    pub fn mapped_name<'a>(&'a self, config: &'a Config) -> &'a str {
        let s = self.name.as_str();
        if let Some(entry) = config.qemu.gdb_reg_map.get(s) {
            entry
        } else {
            &s
        }
    }
}

impl Serialize for SHMRegister {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        let mut s = serializer.serialize_struct("register", 3)?;
        s.serialize_field("size", &self.size)?;
        s.serialize_field("data", &self.data_slice_fmt())?;
        s.serialize_field("name", &self.name)?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct SHMCPU {
    pub idx: u32,
    pub registers_size: usize,
    registers: [SHMRegister; MAX_CPU_REGISTERS],
}

impl SHMCPU {
    pub fn registers_slice(&self) -> &[SHMRegister] {
        &self.registers[..self.registers_size]
    }
}

impl Serialize for SHMCPU {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        let mut s = serializer.serialize_struct("cpu", 3)?;
        s.serialize_field("idx", &self.idx)?;
        s.serialize_field("registers_size", &self.registers_size)?;
        s.serialize_field("registers", &self.registers_slice())?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct InsnData {
    pub size: usize,
    buffer: [u8; MAX_INSN_DATA_SIZE],
}

impl InsnData {
    pub fn buffer_slice(&self) -> &[u8] {
        &self.buffer[..self.size]
    }
}

impl Serialize for InsnData {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        let mut s = serializer.serialize_struct("insn-data", 2)?;
        s.serialize_field("size", &self.size)?;
        s.serialize_field("buffer", &self.buffer_slice())?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Serialize, Clone)]
pub struct TBInsnInfo {
    pub pc: u64,
    pub size: usize,
    pub symbol: SHMString,
    pub hwaddr: SHMString,
    pub disas: SHMString,
    pub data: InsnData,
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct TBInfo {
    pub pc: u64,
    pub insns_info_size: usize,
    insns_info: [TBInsnInfo; TBINSNINFO_ENTRIES],
}

impl TBInfo {
    pub fn insns_info_slice(&self) -> &[TBInsnInfo] {
        &self.insns_info[..self.insns_info_size]
    }
}

impl Serialize for TBInfo {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        let mut s = serializer.serialize_struct("tb-info", 3)?;
        s.serialize_field("pc", &self.pc)?;
        s.serialize_field("insns_info_size", &self.insns_info_size)?;
        s.serialize_field("insns_info", self.insns_info_slice())?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct BrokerSHMTB {
    /// A bit-mask indicating which cpu-indicies are set
    pub init_mask: i32,
    pub cpus: [SHMCPU; MAX_CPU_COUNT],
    pub tb_info: TBInfo,
}

impl Serialize for BrokerSHMTB {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        let mut s = serializer.serialize_struct("shm-tb", 3)?;
        s.serialize_field("init_mask", &self.init_mask)?;

        let mut cpus = vec![];
        for idx in 0..MAX_CPU_COUNT {
            let flag = self.init_mask & (1 << idx);
            if flag == 1 {
                cpus.push(&self.cpus[idx]);
            }
        }

        s.serialize_field("cpus", &cpus)?;
        s.serialize_field("tb_info", &self.tb_info)?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct BrokerSHMExec {
    /// A bit-mask indicating which cpu-indicies are set
    pub init_mask: i32,
    pub cpus: [SHMCPU; MAX_CPU_COUNT],
    pub insn_info: TBInsnInfo,
}

impl Serialize for BrokerSHMExec {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer {
        let mut s = serializer.serialize_struct("shm-tb", 3)?;
        s.serialize_field("init_mask", &self.init_mask)?;

        let mut cpus = vec![];
        for idx in 0..MAX_CPU_COUNT {
            let flag = self.init_mask & (1 << idx);
            if flag == 1 {
                cpus.push(&self.cpus[idx]);
            }
        }

        s.serialize_field("cpus", &cpus)?;
        s.serialize_field("insn_info", &self.insn_info)?;
        s.end()
    }
}

#[repr(C)]
pub union BrokerSHM {
    pub shm_tb: std::mem::ManuallyDrop<BrokerSHMTB>,
    pub shm_exec: std::mem::ManuallyDrop<BrokerSHMExec>,
}
