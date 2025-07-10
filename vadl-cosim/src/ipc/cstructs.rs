#[repr(C)]
#[derive(Debug)]
struct SHMString {
    len: usize,
    value: [u8; SHMSTRING_MAX_LEN],
}

#[repr(C)]
#[derive(Debug)]
struct SHMRegister {
    size: i32,
    data: [u8; MAX_REGISTER_DATA_SIZE],
    name: SHMString,
}

#[repr(C)]
#[derive(Debug)]
struct SHMCPU {
    idx: u32,
    registers_size: usize,
    registers: [SHMRegister; MAX_CPU_REGISTERS],
}

#[repr(C)]
#[derive(Debug)]
struct InsnData {
    size: usize,
    buffer: [u8; MAX_INSN_DATA_SIZE],
}

#[repr(C)]
#[derive(Debug)]
struct TBInsnInfo {
    pc: u64,
    size: usize,
    symbol: SHMString,
    hwaddr: SHMString,
    disas: SHMString,
    data: InsnData,
}

#[repr(C)]
#[derive(Debug)]
struct TBInfo {
    pc: u64,
    insns_info_size: usize,
    insns_info: [TBInsnInfo; TBINSNINFO_ENTRIES],
}

// if bit at cpu_idx = 1 then data is set
#[repr(C)]
#[derive(Debug)]
struct BrokerSHM_TB {
    init_mask: i32,
    cpus: [SHMCPU; MAX_CPU_COUNT],
    tb_info: TBInfo,
}

#[repr(C)]
#[derive(Debug)]
struct BrokerSHM_Exec {
    init_mask: i32,
    cpus: [SHMCPU; MAX_CPU_COUNT],
    insn_info: TBInsnInfo,
}

#[repr(C, packed)]
#[derive(Debug)]
union BrokerSHM {
    shm_tb: BrokerSHM_TB,
    shm_exec: BrokerSHM_Exec,
}
