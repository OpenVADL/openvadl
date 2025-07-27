use crate::{cosim::DBConnection, ipc::cstructs::*};

const CREATES_SQL: &str = include_str!("../../res/creates.sql");
const DROPS_SQL: &str = include_str!("../../res/drops.sql");

pub fn setup_database(pool: &DBConnection) -> Result<(), rusqlite::Error> {
    let tx = pool.unchecked_transaction()?;  
    tx.execute_batch(&format!("{DROPS_SQL}; {CREATES_SQL}"))?;
    tx.commit()?;
    Ok(())
}

pub fn insert_broker_shm_tb(
    pool: &DBConnection,
    broker: &BrokerSHMTB,
) -> Result<i64, rusqlite::Error> {
    let tx = pool.unchecked_transaction()?;

    // Insert TBInfo
    let tb_info_id = insert_tb_info(&tx, &broker.tb_info)?;

    // Insert broker_shm_tb
    tx.execute(
        r#"
        INSERT INTO broker_shm_tb (init_mask, tb_info_id)
        VALUES (?, ?)
        "#,
        (broker.init_mask, tb_info_id),
    )?;

    let broker_id = tx.last_insert_rowid();

    // Insert CPUs and their registers
    for (idx, cpu) in broker.cpus.iter().enumerate() {
        if (broker.init_mask & (1 << idx)) != 0 {
            let cpu_id = insert_shm_cpu(&tx, Some(broker_id), cpu)?;
            insert_shm_registers(&tx, cpu_id, cpu.registers_slice())?;
        }
    }

    tx.commit()?;
    Ok(broker_id)
}

pub fn insert_broker_shm_exec(
    pool: &DBConnection,
    exec: &BrokerSHMExec,
) -> Result<i64, rusqlite::Error> {
    let tx = pool.unchecked_transaction()?;

    // Insert TBInsnInfo
    let insn_info_id = insert_tb_insn_info(&tx, None, &exec.insn_info)?;

    // Insert broker_shm_exec
    tx.execute(
        r#"
        INSERT INTO broker_shm_exec (init_mask, insn_info_id)
        VALUES (?, ?)
        "#,
        (exec.init_mask, insn_info_id),
    )?;

    let broker_id = tx.last_insert_rowid();

    // Insert CPUs and registers
    for (idx, cpu) in exec.cpus.iter().enumerate() {
        if (exec.init_mask & (1 << idx)) != 0 {
            let cpu_id = insert_shm_cpu(&tx, Some(broker_id), cpu)?;
            insert_shm_registers(&tx, cpu_id, cpu.registers_slice())?;
        }
    }

    tx.commit()?;
    Ok(broker_id)
}

fn insert_tb_info(
    tx: &rusqlite::Transaction<'_>,
    tb_info: &TBInfo,
) -> Result<i64, rusqlite::Error> {
    let pc = tb_info.pc as i64;
    let insns_info_size = tb_info.insns_info_size as i64;
    tx.execute(
        "INSERT INTO tb_info (pc, insns_info_size) VALUES (?, ?)",
        (pc, insns_info_size),
    )?;

    let tb_info_id = tx.last_insert_rowid();

    for insn in tb_info.insns_info_slice() {
        insert_tb_insn_info(tx, Some(tb_info_id), insn)?;
    }

    Ok(tb_info_id)
}

fn insert_tb_insn_info(
    tx: &rusqlite::Transaction<'_>,
    tb_info_id: Option<i64>,
    insn: &TBInsnInfo,
) -> Result<i64, rusqlite::Error> {
    let pc = insn.pc as i64;
    let size = insn.size as i64;
    let symbol = insn.symbol.as_str();
    let hwaddr = insn.hwaddr.as_str();
    let disas = insn.disas.as_str();
    let data_size = insn.data.size as i64;
    let data_buffer = insn.data.buffer_slice();

    tx.execute(
        r#"
        INSERT INTO tb_insn_info (tb_info_id, pc, size, symbol, hwaddr, disas, data_size, data)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        "#,
        (
            tb_info_id,
            pc,
            size,
            symbol,
            hwaddr,
            disas,
            data_size,
            data_buffer,
        ),
    )?;

    Ok(tx.last_insert_rowid())
}

fn insert_shm_cpu(
    tx: &rusqlite::Transaction<'_>,
    parent_id: Option<i64>,
    cpu: &SHMCPU,
) -> Result<i64, rusqlite::Error> {
    let idx = cpu.idx as i64;
    let registers_size = cpu.registers_size as i64;

    tx.execute(
        "INSERT INTO shm_cpu (parent_id, idx, registers_size) VALUES (?, ?, ?)",
        (parent_id, idx, registers_size),
    )?;

    Ok(tx.last_insert_rowid())
}

fn insert_shm_registers(
    tx: &rusqlite::Transaction<'_>,
    cpu_id: i64,
    registers: &[SHMRegister],
) -> Result<(), rusqlite::Error> {
    for reg in registers {
        let reg_data = reg.data_slice();
        let reg_name = reg.name.as_str();

        tx.execute(
            "INSERT INTO shm_register (cpu_id, size, data, name) VALUES (?, ?, ?, ?)",
            (cpu_id, reg.size, reg_data, reg_name),
        )?;
    }
    Ok(())
}
