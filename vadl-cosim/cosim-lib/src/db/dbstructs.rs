use crate::config::Config;

#[derive(Debug, Clone)]
pub struct Client {
    pub id: i32,
    pub name: Option<String>,
}

impl Client {
    pub fn new(id: i32, name: Option<String>) -> Self {
        Self { id, name }
    }
}

#[derive(Debug, Clone)]
pub struct ClientEntry {
    pub client: Client,
    pub broker: BrokerData,
    pub run_count: u64,
}

#[derive(Debug, Clone)]
pub enum BrokerData {
    TB(BrokerTB),
    Insn(BrokerInsn),
}

impl BrokerData {
    pub fn data(&self) -> &Vec<CPU> {
        match self {
            BrokerData::TB(broker_tb) => &broker_tb.cpus,
            BrokerData::Insn(broker_insn) => &broker_insn.cpus,
        }
    }
}

impl From<BrokerTB> for BrokerData {
    fn from(value: BrokerTB) -> Self {
        BrokerData::TB(value)
    }
}

impl From<BrokerInsn> for BrokerData {
    fn from(value: BrokerInsn) -> Self {
        BrokerData::Insn(value)
    }
}

impl ClientEntry {
    pub fn new(client: Client, broker: BrokerData, run_count: u64) -> Self {
        Self {
            client,
            broker,
            run_count,
        }
    }
}

#[derive(Debug, Clone)]
pub struct Register {
    pub size: i32,
    pub data: Vec<u8>,
    pub name: String,
}

impl Register {
    pub fn new(size: i32, data: Vec<u8>, name: String) -> Self {
        Self { size, data, name }
    }

    pub fn data_fmt(&self) -> String {
        let s = self
            .data
            .iter()
            .map(|b| format!("{b:02X?}"))
            .collect::<String>();

        format!("0x{s}")
    }

    pub fn data_int(&self) -> u128 {
        let mut res = 0;
        for b in &self.data {
            res = (res << 8) | *b as u128
        }
        res
    }

    pub fn mapped_name<'a>(&'a self, config: &'a Config) -> &'a str {
        let s = self.name.as_str();
        if let Some(entry) = config.qemu.gdb_reg_map.get(s) {
            entry
        } else {
            s
        }
    }
}

#[derive(Debug, Clone)]
pub struct CPU {
    pub idx: u32,
    pub registers: Vec<Register>,
}

impl CPU {
    pub fn new(idx: u32, registers: Vec<Register>) -> Self {
        Self { idx, registers }
    }
}

#[derive(Debug, Clone)]
pub struct TBInsnInfo {
    pub pc: u64,
    pub size: usize,
    pub symbol: String,
    pub hwaddr: String,
    pub disas: String,
    pub data: Vec<u8>,
}

impl TBInsnInfo {
    pub fn new(
        pc: u64,
        size: usize,
        symbol: String,
        hwaddr: String,
        disas: String,
        data: Vec<u8>,
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

    pub fn data_fmt(&self) -> String {
        let s = self
            .data
            .iter()
            .map(|b| format!("{b:02X?}"))
            .collect::<String>();

        format!("0x{s}")
    }
}

#[derive(Debug, Clone)]
pub struct TBInfo {
    pub pc: u64,
    pub insns_info: Vec<TBInsnInfo>,
}

impl TBInfo {
    pub fn new(pc: u64, insns_info: Vec<TBInsnInfo>) -> Self {
        Self { pc, insns_info }
    }
}

#[derive(Debug, Clone)]
pub struct BrokerTB {
    /// A bit-mask indicating which cpu-indicies are set
    pub init_mask: i32,
    pub cpus: Vec<CPU>,
    pub tb_info: TBInfo,
}

impl BrokerTB {
    pub fn new(init_mask: i32, cpus: Vec<CPU>, tb_info: TBInfo) -> Self {
        Self {
            init_mask,
            cpus,
            tb_info,
        }
    }
}

#[derive(Debug, Clone)]
pub struct BrokerInsn {
    /// A bit-mask indicating which cpu-indicies are set
    pub init_mask: i32,
    pub cpus: Vec<CPU>,
    pub insn_info: TBInsnInfo,
}

impl BrokerInsn {
    pub fn new(init_mask: i32, cpus: Vec<CPU>, insn_info: TBInsnInfo) -> Self {
        Self {
            init_mask,
            cpus,
            insn_info,
        }
    }
}
