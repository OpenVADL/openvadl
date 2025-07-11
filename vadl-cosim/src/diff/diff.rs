use crate::{
    config::Config,
    diff::{DiffContext, DiffEntry},
    ipc::cstructs::{SHMRegister, MAX_CPU_COUNT, SHMCPU},
};

pub fn diff_cpus(
    cpus1: &[SHMCPU; MAX_CPU_COUNT],
    init_mask1: i32,
    cpus2: &[SHMCPU; MAX_CPU_COUNT],
    init_mask2: i32,
    config: &Config,
    context: DiffContext,
) -> Vec<DiffEntry> {
    let mut diffs = vec![];

    if init_mask1 != init_mask2 {
        diffs.push(DiffEntry::new(
            "cpu.init_mask",
            vec![init_mask1.to_string(), init_mask2.to_string()],
            "The init masks of the cpu differ - meaning different CPUs were used during execution",
            context,
        ));

        return diffs;
    }

    for idx in 0..MAX_CPU_COUNT {
        let flag = init_mask1 & (1 << idx);
        if flag == 1 {
            let cpu1 = &cpus1[idx];
            let cpu2 = &cpus2[idx];
            diffs.append(&mut diff_cpu(cpu1, cpu2, idx, config, context.clone()));
        }
    }

    diffs
}

pub fn diff_cpu(cpu1: &SHMCPU, cpu2: &SHMCPU, cpu_index: usize, config: &Config, context: DiffContext) -> Vec<DiffEntry> {
    let mut diffs = vec![];

    if !config.qemu.ignore_unset_registers && cpu1.registers_size != cpu2.registers_size {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers.size"),
            vec![
                cpu1.registers_size.to_string(),
                cpu2.registers_size.to_string(),
            ],
            "different number of CPU registers",
            context,
        ));

        return diffs;
    }

    let mut cpus = [cpu1, cpu2];
    cpus.sort_by_key(|c| c.registers_size);
    let [sub_cpu, super_cpu] = cpus;

    for reg_index in 0..sub_cpu.registers_size {
        let csub_reg = &sub_cpu.registers_slice()[reg_index];
        let rsub_name = csub_reg.mapped_name(config);

        if config.qemu.ignore_registers.contains(&rsub_name) {
            continue;
        }

        // TODO: maybe store an inverted map
        if config.qemu.ignore_unset_registers
            && !config
                .qemu
                .gdb_reg_map
                .values()
                .collect::<Vec<_>>()
                .contains(&&rsub_name)
        {
            continue;
        }

        let csuper_reg = reg_by_name(
            super_cpu.registers_slice(),
            &csub_reg.name.to_string(),
            config,
        );
        diffs.append(&mut diff_register(
            csub_reg,
            csuper_reg,
            cpu_index,
            reg_index,
            config,
            context.clone(),
        ));
    }

    diffs
}

pub fn diff_register(
    reg1: &SHMRegister,
    reg2: &SHMRegister,
    cpu_index: usize,
    reg_index: usize,
    config: &Config,
    context: DiffContext,
) -> Vec<DiffEntry> {
    let mut diffs = vec![];

    let r1name = reg1.mapped_name(config);
    let r2name = reg2.mapped_name(config);

    if reg1.size != reg2.size {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].size"),
            vec![reg1.size.to_string(), reg2.size.to_string()],
            format!("different register sizes for {r1name}"),
            context.clone(),
        ));
    }

    if r1name != r2name {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].name"),
            vec![r1name.clone(), r2name.clone()],
            "different register names",
            context.clone(),
        ));
    }

    if reg1.data_slice() != reg2.data_slice() {
        diffs.push(DiffEntry::new(
            format!("cpu[{cpu_index}].registers[{reg_index}].data"),
            vec![reg1.data_slice_fmt(), reg2.data_slice_fmt()],
            format!("different register data for {r1name}"),
            context.clone(),
        ));
    }

    diffs
}

fn reg_by_name<'a>(registers: &'a [SHMRegister], name: &str, config: &Config) -> &'a SHMRegister {
    for reg in registers {
        let reg_name = reg.mapped_name(config);
        if reg_name == name {
            return reg;
        }

        if let Some(mapped_name) = config.qemu.gdb_reg_map.get(name)
            && reg_name == *mapped_name
        {
            return reg;
        }
    }

    panic!("reg_by_name called but no register with that name found: {name}");
}
