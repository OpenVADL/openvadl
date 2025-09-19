use std::cell::RefCell;

use crate::{
    config::Config,
    diff::DiffEntry,
    ipc::cstructs::{MAX_CPU_COUNT, MAX_CPU_REGISTERS, SHMCPU, SHMRegister},
};

// NOTE: Technically it is more performant to pass in the memo-vec as an argument to not have
// dynamic borrow-checks. However this currently doesn't measurably impact runtime-performance.
// In the future it might be useful to wrap these functions in a struct in which case the
// memoization can be managed via that.
thread_local! {
    static REG_MAP_MEMO: RefCell<Vec<Option<usize>>> = RefCell::new(vec![None; MAX_CPU_REGISTERS]);
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
        if flag == 1 {
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
            && !config.qemu.gdb_reg_map_inverse.contains_key(rsub_name)
        {
            continue;
        }

        let csuper_reg_idx = reg_idx_by_name_memoed(
            super_cpu.registers_slice(),
            csub_reg.name.as_str(),
            config,
            reg_index,
        );
        let csuper_reg = &super_cpu.registers_slice()[csuper_reg_idx];

        diff_register(csub_reg, csuper_reg, cpu_index, reg_index, config, diffs);
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
    let r1name = reg1.mapped_name(config);
    let r2name = reg2.mapped_name(config);

    if reg1.size != reg2.size {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].size"),
            vec![reg1.size.to_string(), reg2.size.to_string()],
            format!("different register sizes for {r1name}"),
        ));
    }

    if r1name != r2name {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].name"),
            vec![r1name.to_string(), r2name.to_string()],
            "different register names",
        ));
    }

    for idx in 0..(reg1.size as usize) {
        let d1 = reg1.data_slice()[idx];
        let d2 = if config.qemu.has_equal_endianess() {
            reg2.data_slice()[idx]
        } else {
            reg2.data_slice()[(reg2.size as usize) - idx - 1]
        };

        if d1 != d2 {
            diffs.push(DiffEntry::new(
                format!("cpu[{cpu_index}].registers[{reg_index}].data"),
                vec![reg1.data_slice_fmt(), reg2.data_slice_fmt()],
                format!("different register data for {r1name}"),
            ));

            break;
        }
    }
}

fn reg_idx_by_name_memoed(
    registers: &[SHMRegister],
    name: &str,
    config: &Config,
    reg_index: usize,
) -> usize {
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

fn reg_idx_by_name(registers: &[SHMRegister], name: &str, config: &Config) -> usize {
    for (idx, reg) in registers.iter().enumerate() {
        let reg_name = reg.mapped_name(config);
        if reg_name == name {
            return idx;
        }

        if let Some(mapped_name) = config.qemu.gdb_reg_map.get(name)
            && reg_name == *mapped_name
        {
            return idx;
        }
    }

    panic!("reg_by_name called but no register with that name found: {name}");
}
