use std::{mem, time::Duration};

use color_eyre::{Result, eyre::bail};
use serde::{Serialize, ser::SerializeStruct};
use tracing::debug;

use crate::{
    config::{Config, Endian},
    ipc::sem::Semaphore,
};

pub const SHMSTRING_MAX_LEN: usize = 256;
pub const TBINSNINFO_ENTRIES: usize = 64;
pub const MAX_REGISTER_NAME_SIZE: usize = 64;
pub const MAX_REGISTER_DATA_SIZE: usize = 256;
pub const MAX_CPU_REGISTERS: usize = 512;
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
        S: serde::Serializer,
    {
        serializer.serialize_str(self.as_str())
    }
}

impl SHMString {
    pub fn new(len: usize, value: [u8; SHMSTRING_MAX_LEN]) -> Self {
        Self { len, value }
    }

    pub fn as_str(&self) -> &str {
        std::str::from_utf8(&self.value[..self.len]).expect("valid utf8 sequence in SHMString")
    }
}

impl From<String> for SHMString {
    fn from(value: String) -> Self {
        let len = value.len();
        assert!(len < SHMSTRING_MAX_LEN);
        let mut slice = [0u8; SHMSTRING_MAX_LEN];
        slice[..len].copy_from_slice(value.as_bytes());
        Self::new(len, slice)
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
    pub fn new(size: i32, data: [u8; MAX_REGISTER_DATA_SIZE], name: SHMString) -> Self {
        Self { size, data, name }
    }

    pub fn data_slice(&self) -> &[u8] {
        &self.data[..self.size as usize]
    }

    pub fn data_slice_fmt(&self) -> String {
        let s = self
            .data_slice()
            .iter()
            .map(|b| format!("{b:02X?}"))
            .collect::<String>();

        format!("0x{s}")
    }

    pub fn mapped_name<'a>(&'a self, config: &'a Config) -> &'a str {
        let s = self.name.as_str();
        if let Some(entry) = config.qemu.gdb_reg_map.get(s) {
            entry
        } else {
            s
        }
    }

    pub fn to_u64(&self, endian: &Endian) -> u64 {
        const BUF_LEN: usize = 8;
        let mut buf: [u8; BUF_LEN] = [0; BUF_LEN];
        match endian {
            Endian::Little => {
                buf[..self.size as usize].copy_from_slice(self.data_slice());
                u64::from_le_bytes(buf)
            }
            Endian::Big => {
                buf[BUF_LEN - self.size as usize..].copy_from_slice(self.data_slice());
                u64::from_be_bytes(buf)
            }
        }
    }
}

impl Serialize for SHMRegister {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
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
    pub fn new(
        idx: u32,
        registers_size: usize,
        registers: [SHMRegister; MAX_CPU_REGISTERS],
    ) -> Self {
        Self {
            idx,
            registers_size,
            registers,
        }
    }

    pub fn registers_slice(&self) -> &[SHMRegister] {
        &self.registers[..self.registers_size]
    }
}

impl Serialize for SHMCPU {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
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
    pub fn new(size: usize, buffer: [u8; MAX_INSN_DATA_SIZE]) -> Self {
        Self { size, buffer }
    }

    pub fn buffer_slice(&self) -> &[u8] {
        &self.buffer[..self.size]
    }

    pub fn buffer_slice_fmt(&self) -> String {
        let s = self
            .buffer_slice()
            .iter()
            .map(|b| format!("{b:02X?}"))
            .collect::<String>();

        format!("0x{s}")
    }
}

impl Serialize for InsnData {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let mut s = serializer.serialize_struct("insn-data", 2)?;
        s.serialize_field("size", &self.size)?;
        s.serialize_field("buffer", &self.buffer_slice())?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct TBInsnInfo {
    pub pc: u64,
    pub size: usize,
    pub symbol: SHMString,
    pub hwaddr: SHMString,
    pub disas: SHMString,
    pub data: InsnData,
}

impl TBInsnInfo {
    pub fn new(
        pc: u64,
        size: usize,
        symbol: SHMString,
        hwaddr: SHMString,
        disas: SHMString,
        data: InsnData,
    ) -> Self {
        Self {
            pc,
            size,
            symbol,
            hwaddr,
            disas,
            data,
        }
    }
}

impl Serialize for TBInsnInfo {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let mut s = serializer.serialize_struct("tb-insn-info", 6)?;
        s.serialize_field("pc", &self.pc)?;
        s.serialize_field("size", &self.size)?;
        s.serialize_field("symbol", &self.symbol)?;
        s.serialize_field("hwaddr", &self.hwaddr)?;
        s.serialize_field("disas", &self.disas)?;
        s.serialize_field("data", &self.data)?;
        s.end()
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct TBInfo {
    pub pc: u64,
    pub insns_info_size: usize,
    insns_info: [TBInsnInfo; TBINSNINFO_ENTRIES],
}

impl TBInfo {
    pub fn new(
        pc: u64,
        insns_info_size: usize,
        insns_info: [TBInsnInfo; TBINSNINFO_ENTRIES],
    ) -> Self {
        Self {
            pc,
            insns_info_size,
            insns_info,
        }
    }

    pub fn insns_info_slice(&self) -> &[TBInsnInfo] {
        &self.insns_info[..self.insns_info_size]
    }
}

impl Serialize for TBInfo {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
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
        S: serde::Serializer,
    {
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

impl BrokerSHMTB {
    pub fn new(init_mask: i32, cpus: [SHMCPU; MAX_CPU_COUNT], tb_info: TBInfo) -> Self {
        Self {
            init_mask,
            cpus,
            tb_info,
        }
    }
}

#[repr(C)]
#[derive(Debug, Clone, Serialize)]
pub enum BrokerSHMInsnDataType {
    InsnExec = 0,
    InsnMem = 1 << 0,
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct BrokerSHMInsn {
    /// A bit-mask indicating which cpu-indicies are set
    pub init_mask: i32,
    pub insn_data_type: BrokerSHMInsnDataType,
    pub cpus: [SHMCPU; MAX_CPU_COUNT],
    pub insn_info: TBInsnInfo,
    pub mem_access_info: MemAccessInfo,
}

impl Serialize for BrokerSHMInsn {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let mut s = serializer.serialize_struct("shm-tb", 5)?;
        s.serialize_field("init_mask", &self.init_mask)?;
        s.serialize_field("insn_data_type", &self.insn_data_type)?;

        if let Some(cpus) = self.cpus() {
            let mut used_cpus = vec![];
            #[allow(clippy::needless_range_loop)]
            for idx in 0..MAX_CPU_COUNT {
                let flag = self.init_mask & (1 << idx);
                if flag == 1 {
                    used_cpus.push(&cpus[idx]);
                }
            }
            s.serialize_field("cpus", &Some(used_cpus))?;
        } else {
            s.serialize_field("cpus", &None::<Vec<&SHMCPU>>)?;
        }

        s.serialize_field("insn_info", &self.insn_info)?;
        s.serialize_field("mem_access_info", &self.mem_access_info())?;

        s.end()
    }
}

impl BrokerSHMInsn {
    pub fn new(
        init_mask: i32,
        insn_data_type: BrokerSHMInsnDataType,
        cpus: [SHMCPU; MAX_CPU_COUNT],
        insn_info: TBInsnInfo,
        mem_access_info: MemAccessInfo,
    ) -> Self {
        Self {
            init_mask,
            insn_data_type,
            cpus,
            insn_info,
            mem_access_info,
        }
    }

    pub fn cpus(&self) -> Option<&[SHMCPU; MAX_CPU_COUNT]> {
        match self.insn_data_type {
            BrokerSHMInsnDataType::InsnExec => Some(&self.cpus),
            BrokerSHMInsnDataType::InsnMem => None,
        }
    }

    pub fn mem_access_info(&self) -> Option<&MemAccessInfo> {
        match self.insn_data_type {
            BrokerSHMInsnDataType::InsnExec => None,
            BrokerSHMInsnDataType::InsnMem => Some(&self.mem_access_info),
        }
    }
}

#[repr(C)]
#[derive(Debug, Clone)]
pub struct MemAccessInfo {
    pub vaddr: u64,
    // the size of the memory load / store in ^2: 0 = 1 byte, 1 = 2 bytes, ..., 4
    // = 16 bytes
    pub size: u8,
    // the amount written to the data-array depends on the size
    data: [u8; 16],
    pub is_store: bool,
}

impl Serialize for MemAccessInfo {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let mut s = serializer.serialize_struct("shm-mem", 4)?;
        s.serialize_field("size", &self.size)?;
        s.serialize_field("data", &self.data)?;
        s.serialize_field("vaddr", &self.vaddr)?;
        s.serialize_field("is_store", &self.is_store)?;
        s.end()
    }
}

impl MemAccessInfo {
    const BUF_LEN: usize = 16;

    pub fn new(size: u8, data: [u8; Self::BUF_LEN], vaddr: u64, is_store: bool) -> Self {
        Self {
            size,
            data,
            vaddr,
            is_store,
        }
    }

    pub fn data_slice(&self) -> &[u8] {
        let bytes: usize = 1 << self.size;
        &self.data[..bytes]
    }

    pub fn data_u128(&self) -> u128 {
        u128::from_ne_bytes(self.data)
    }

    pub fn data_slice_fmt(&self) -> String {
        let s = self
            .data_slice()
            .iter()
            .map(|b| format!("{b:02X?}"))
            .collect::<String>();

        format!("0x{s}")
    }

    pub fn to_u128(&self, endian: &Endian) -> u128 {
        let mut buf: [u8; Self::BUF_LEN] = [0; Self::BUF_LEN];
        let source_slice = self.data_slice();
        match endian {
            Endian::Little => {
                buf[..source_slice.len()].copy_from_slice(source_slice);
                u128::from_le_bytes(buf)
            }
            Endian::Big => {
                buf[Self::BUF_LEN - source_slice.len()..].copy_from_slice(source_slice);
                u128::from_be_bytes(buf)
            }
        }
    }
}

#[repr(C)]
pub union BrokerSHMData {
    pub shm_tb: std::mem::ManuallyDrop<BrokerSHMTB>,
    pub shm_insn: std::mem::ManuallyDrop<BrokerSHMInsn>,
}

impl BrokerSHMData {
    pub fn as_tb(&self) -> &BrokerSHMTB {
        unsafe { &self.shm_tb }
    }

    pub fn as_insn(&self) -> &BrokerSHMInsn {
        unsafe { &self.shm_insn }
    }
}

#[repr(C)]
pub struct BrokerSHMRingBuffer<const SIZE: usize> {
    pub data: [BrokerSHMData; SIZE],
    pub read_idx: usize,
    pub write_idx: usize,
    pub count: usize,
    pub write_end: bool,
    pub rb_mutex: Semaphore,
}

impl<const SIZE: usize> BrokerSHMRingBuffer<SIZE> {
    const MASK: usize = SIZE - 1;

    pub fn init(&mut self) -> Result<()> {
        debug!("init ringbuffer with size: {SIZE}");

        self.data = unsafe { mem::zeroed() };
        self.read_idx = 0;
        self.write_idx = 0;
        self.count = 0;
        self.write_end = false;
        self.rb_mutex = Semaphore::create()?;
        Ok(())
    }

    pub const fn current_size(&self, read_idx: usize, write_idx: usize) -> usize {
        (write_idx - read_idx) & ((Self::MASK << 1) | 1)
    }

    pub const fn ring_idx(&self, idx: usize) -> usize {
        idx & Self::MASK
    }

    pub const fn writer_is_closed(&self) -> bool {
        self.write_idx == usize::MAX
    }

    /// Tries to read the next entry from the ringbuffer
    /// An error is returned if timedwait returns an error (timeout or other)
    /// When no more entries are expected (count == 0 and write_idx == usize::MAX) None is
    /// returned.
    /// Otherwise a reference to the next data is returned.
    ///
    /// NOTE: `end_read` has to be called once the reference is not needed anymore to free the
    /// index in the ringbuffer for new writes.
    pub fn start_read(&mut self) -> Result<Option<&BrokerSHMData>> {
        // NOTE: Using manual pointer dereferencing seems to be necessary here since otherwise, due
        // to the move semantics (usize and bool implementing Copy), rust only copies the initial
        // value into the closure, which is not wanted. Using this unsafe deref ensures that always
        // the current value is read - which is safe in this case because the read is guarded
        // behind a mutex.
        let wref = &self.write_end as *const _;
        let cref = &self.count as *const _;
        let waitres = self.rb_mutex.timedwait(Duration::from_secs(1), || unsafe {
            *wref || *cref > 0usize
        })?;

        match waitres {
            crate::ipc::sem::TimedWaitState::Timeout => {
                bail!(
                    "Failed to wait for a response from a qemu client. Note that a timeout should *not* mean that the client crashed - it is more likely that the client is in an endless loop or that the cosimulator has a bug. If the test failure is not repeatable, then it is most likely a cosimulator bug! Please refer to the logs for more information."
                );
            }
            crate::ipc::sem::TimedWaitState::Success => {
                // If we successfully got a "response" from the writer, but the count is
                // still zero, then that means that the response was a write-end message.
                // Meaning all data was already read
                if self.count == 0 {
                    self.rb_mutex.unlock()?;
                    return Ok(None);
                }
            }
        }

        self.rb_mutex.unlock()?;
        let idx = self.ring_idx(self.read_idx);
        let elem = &self.data[idx];

        Ok(Some(elem))
    }

    pub const fn read_this(&self) -> &BrokerSHMData {
        let idx = self.ring_idx(self.read_idx);
        &self.data[idx]
    }

    // NOTE: to ensure that the previous entry is still valid (i.e. not a new value) the function
    // has to be called *before* `end_read`.
    //
    // Once end_read is called, the reference is no longer valid, therefore the value needs to
    // already be used or cloned once `end_read` is called.
    pub const fn read_previous(&self) -> &BrokerSHMData {
        let idx = self.ring_idx(self.read_idx.wrapping_sub(1));
        &self.data[idx]
    }

    pub fn end_read(&mut self) -> Result<()> {
        self.read_idx += 1;
        self.rb_mutex.lock()?;
        self.count -= 1;
        self.rb_mutex.unlock()?;
        self.rb_mutex.signal()?;
        Ok(())
    }
}
