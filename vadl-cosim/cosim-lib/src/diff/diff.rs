use std::cell::RefCell;

use crate::{
    config::{Config, SlicedReg},
    diff::DiffEntry,
    ipc::cstructs::{MAX_CPU_COUNT, MAX_CPU_REGISTERS, MemAccessInfo, SHMCPU, SHMRegister},
};

// NOTE: Technically it is more performant to pass in the memo-vec as an argument to not have
// dynamic borrow-checks. However this currently doesn't measurably impact runtime-performance.
// In the future it might be useful to wrap these functions in a struct in which case the
// memoization can be managed via that.
thread_local! {
    static REG_MAP_MEMO: RefCell<Vec<Option<Option<usize>>>> = RefCell::new(vec![None; MAX_CPU_REGISTERS]);
}

pub fn diff_mem_access(
    mem_access_info1: &MemAccessInfo,
    mem_access_info2: &MemAccessInfo,
    _config: &Config,
) -> Vec<DiffEntry> {
    let mut diffs = vec![];

    if mem_access_info1.size != mem_access_info2.size {
        diffs.push(DiffEntry::new(
            "mem.size",
            vec![
                mem_access_info1.size.to_string(),
                mem_access_info2.size.to_string(),
            ],
            "The size of a memory-access did not match",
        ));
    }

    if mem_access_info1.vaddr != mem_access_info2.vaddr {
        diffs.push(DiffEntry::new(
            "mem.vaddr",
            vec![
                mem_access_info1.vaddr.to_string(),
                mem_access_info2.vaddr.to_string(),
            ],
            "The (virtual-)address of a memory-access did not match",
        ));
    }

    if mem_access_info1.data_slice() != mem_access_info2.data_slice() {
        diffs.push(DiffEntry::new(
            "mem.data",
            vec![
                mem_access_info1.data_slice_fmt(),
                mem_access_info2.data_slice_fmt(),
            ],
            "Memory-Access data did not match",
        ));
    }

    diffs
}

pub fn diff_cpus(
    cpus1: &[SHMCPU; MAX_CPU_COUNT],
    init_mask1: i32,
    cpus2: &[SHMCPU; MAX_CPU_COUNT],
    init_mask2: i32,
    config: &Config,
) -> Vec<DiffEntry> {
    let mut diffs = vec![];

    if init_mask1 != init_mask2 {
        diffs.push(DiffEntry::new(
            "cpu.init_mask",
            vec![init_mask1.to_string(), init_mask2.to_string()],
            "The init masks of the cpu differ - meaning different CPUs were used during execution",
        ));

        return diffs;
    }

    for idx in 0..MAX_CPU_COUNT {
        let flag = init_mask1 & (1 << idx);
        if flag != 0 {
            let cpu1 = &cpus1[idx];
            let cpu2 = &cpus2[idx];
            diff_cpu(cpu1, cpu2, idx, config, &mut diffs);
        }
    }

    diffs
}

pub fn diff_cpu(
    cpu1: &SHMCPU,
    cpu2: &SHMCPU,
    cpu_index: usize,
    config: &Config,
    diffs: &mut Vec<DiffEntry>,
) {
    if !config.qemu.ignore_unset_registers && cpu1.registers_size != cpu2.registers_size {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers.size"),
            vec![
                cpu1.registers_size.to_string(),
                cpu2.registers_size.to_string(),
            ],
            "different number of CPU registers",
        ));

        return;
    }

    let mut cpus = [cpu1, cpu2];
    cpus.sort_by_key(|c| c.registers_size);
    let [sub_cpu, super_cpu] = cpus;

    for reg_index in 0..sub_cpu.registers_size {
        let csub_reg = &sub_cpu.registers_slice()[reg_index];
        let rsub_name = csub_reg.mapped_name(config);

        if config.qemu.ignore_registers.contains(rsub_name) {
            continue;
        }

        if config.qemu.ignore_unset_registers
            && !config.qemu.defined_registers_map.contains(rsub_name)
        {
            continue;
        }

        let csuper_reg_idx = reg_idx_by_name_memoed(
            super_cpu.registers_slice(),
            csub_reg.name.as_str(),
            config,
            reg_index,
        );

        match csuper_reg_idx {
            Some(csuper_reg_idx) => {
                let csuper_reg = &super_cpu.registers_slice()[csuper_reg_idx];
                diff_register(csub_reg, csuper_reg, cpu_index, reg_index, config, diffs);
            }
            None => {
                // there is no 1:1 mapping -> check 1:n slice-mappings
                let mappings =
                    sliced_register_mappings(super_cpu.registers_slice(), csub_reg, config);

                for (reg1, slice_info1, reg2, slice_info2) in mappings {
                    diff_sliced_register(
                        reg1,
                        slice_info1,
                        reg2,
                        slice_info2,
                        cpu_index,
                        reg_index,
                        config,
                        diffs,
                    );
                }
            }
        };
    }
}

pub fn diff_register(
    reg1: &SHMRegister,
    reg2: &SHMRegister,
    cpu_index: usize,
    reg_index: usize,
    config: &Config,
    diffs: &mut Vec<DiffEntry>,
) {
    let reg1val = reg1.to_u64(&config.qemu.clients[0].endian);
    let reg2val = reg2.to_u64(&config.qemu.clients[1].endian);

    if reg1val != reg2val {
        let r1name = reg1.mapped_name(config);
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].data"),
            vec![reg1.data_slice_fmt(), reg2.data_slice_fmt()],
            format!("different register data for {r1name}"),
        ));
    }
}

#[allow(clippy::too_many_arguments)]
pub fn diff_sliced_register(
    reg1: &SHMRegister,
    slice_info1: &SlicedReg,
    reg2: &SHMRegister,
    slice_info2: &SlicedReg,
    cpu_index: usize,
    reg_index: usize,
    config: &Config,
    diffs: &mut Vec<DiffEntry>,
) {
    let mut reg1val = reg1.to_u64(&config.qemu.clients[0].endian);
    slice_info1.apply(&mut reg1val);

    let mut reg2val = reg2.to_u64(&config.qemu.clients[1].endian);
    slice_info2.apply(&mut reg2val);

    if reg1val != reg2val {
        let r1name = reg1.mapped_name(config);
        let r2name = reg2.mapped_name(config);
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].data"),
            vec![reg1.data_slice_fmt(), reg2.data_slice_fmt()],
            format!("different (sliced) register data for {r1name} (sliced to {reg1val}) and {r2name} (sliced to {reg2val})"),
        ));
    }
}

fn reg_idx_by_name_memoed(
    registers: &[SHMRegister],
    name: &str,
    config: &Config,
    reg_index: usize,
) -> Option<usize> {
    REG_MAP_MEMO.with_borrow_mut(|memo| {
        assert!(reg_index < memo.len(), "invalid memo length");
        if let Some(memo_reg) = memo[reg_index] {
            return memo_reg;
        }

        let reg = reg_idx_by_name(registers, name, config);
        memo[reg_index] = Some(reg);
        reg
    })
}

fn reg_idx_by_name(registers: &[SHMRegister], name: &str, config: &Config) -> Option<usize> {
    for (idx, reg) in registers.iter().enumerate() {
        let reg_name = reg.mapped_name(config);
        if reg_name == name {
            return Some(idx);
        }

        if let Some(mapped_name) = config.qemu.gdb_reg_map.get(name)
            && reg_name == *mapped_name
        {
            return Some(idx);
        }
    }

    None
}

fn sliced_register_mappings<'a, 'b>(
    super_registers: &'a [SHMRegister],
    sub_reg: &'a SHMRegister,
    config: &'b Config,
) -> Vec<(
    &'a SHMRegister,
    &'b SlicedReg,
    &'a SHMRegister,
    &'b SlicedReg,
)> {
    let mappings = config
        .qemu
        .sliced_reg_map
        .get_mappings_for(sub_reg.name.as_str());
    let mut register_mappings = vec![];

    if mappings.is_empty() {
        return register_mappings;
    }

    if mappings[0].client1.name == sub_reg.name.as_str() {
        for reg in super_registers {
            for mapping in &mappings {
                if mapping.client2.name == reg.name.as_str() {
                    register_mappings.push((sub_reg, &mapping.client1, reg, &mapping.client2));
                }
            }
        }
    } else {
        for reg in super_registers {
            for mapping in &mappings {
                if mapping.client1.name == reg.name.as_str() {
                    register_mappings.push((sub_reg, &mapping.client2, reg, &mapping.client1));
                }
            }
        }
    }

    register_mappings
}
