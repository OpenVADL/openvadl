use rusqlite::{Transaction, params};

use crate::{
    cosim::DBConnection,
    ipc::{cstructs::*, qemu},
};

const CREATES_SQL: &str = include_str!("../../res/creates.sql");
const DROPS_SQL: &str = include_str!("../../res/drops.sql");

pub fn setup_database(pool: &mut DBConnection) -> Result<(), rusqlite::Error> {
    let tx = pool.transaction()?;
    tx.execute_batch(&format!("{DROPS_SQL}; {CREATES_SQL}"))?;
    tx.commit()?;
    Ok(())
}

const SHMTYPE_TB: u8 = 0;
const SHMTYPE_INSN: u8 = 1;

pub struct CosimRunInfo {
    pub run_id: i64,
    pub client_ids: Vec<i64>,
}

pub fn insert_new_cosimulation_run(
    pool: &mut DBConnection,
    clients: &Vec<qemu::Client>,
) -> Result<CosimRunInfo, rusqlite::Error> {
    let tx = pool.transaction()?;

    tx.execute(r#"INSERT INTO cosimulation_run DEFAULT VALUES"#, params![])?;
    let run_id = tx.last_insert_rowid();

    let mut client_ids = vec![];
    for client in clients {
        let client_id = insert_client(&tx, client.name.clone())?;
        insert_cosim_run_client(&tx, run_id, client_id)?;

        client_ids.push(client_id);
    }

    tx.commit()?;
    Ok(CosimRunInfo { run_id, client_ids })
}

pub fn finish_cosimulation_run_trace(
    pool: &mut DBConnection,
    run: CosimRunInfo,
    passed: bool,
) -> Result<(), rusqlite::Error> {
    pool.execute(
        r#"UPDATE cosimulation_run SET end = CURRENT_TIMESTAMP, passed = ? WHERE id = ?"#,
        params![passed, run.run_id],
    )?;
    Ok(())
}

pub fn insert_client(tx: &Transaction<'_>, name: Option<String>) -> Result<i64, rusqlite::Error> {
    tx.execute(r#"INSERT INTO client(name) VALUES (?)"#, params![name])?;

    Ok(tx.last_insert_rowid())
}

pub fn insert_client_entry(
    pool: &mut DBConnection,
    client_id: i64,
    broker_id: i64,
) -> Result<(), rusqlite::Error> {
    pool.execute(
        r#"INSERT INTO client_entry(client_id, broker_shm_id) VALUES (?, ?)"#,
        params![client_id, broker_id],
    )?;
    Ok(())
}

pub fn insert_cosim_run_client(
    tx: &rusqlite::Transaction<'_>,
    run_id: i64,
    client_id: i64,
) -> Result<(), rusqlite::Error> {
    tx.execute(
        r#"INSERT INTO cosimulation_run_clients(run_id, client_id) VALUES (?, ?)"#,
        params![run_id, client_id],
    )?;
    Ok(())
}

pub fn insert_broker_shm_tb(
    pool: &mut DBConnection,
    broker: &BrokerSHMTB,
) -> Result<i64, rusqlite::Error> {
    let tx = pool.transaction()?;

    // Insert TBInfo
    let tb_info_id = insert_tb_info(&tx, &broker.tb_info)?;

    // Insert broker_shm_tb
    tx.execute(
        r#"
        INSERT INTO broker_shm (init_mask, shm_type)
        VALUES (?, ?)
        "#,
        params![broker.init_mask, SHMTYPE_TB],
    )?;

    let broker_id = tx.last_insert_rowid();

    tx.execute(
        r#"
           INSERT INTO broker_shm_tb (id, tb_info_id) 
           VALUES (?, ?)
        "#,
        params![broker_id, tb_info_id],
    )?;

    // Insert CPUs and their registers
    for (idx, cpu) in broker.cpus.iter().enumerate() {
        if (broker.init_mask & (1 << idx)) != 0 {
            let cpu_id = insert_shm_cpu(&tx, broker_id, cpu)?;
            insert_shm_registers(&tx, cpu_id, cpu.registers_slice())?;
        }
    }

    tx.commit()?;
    Ok(broker_id)
}

pub fn insert_broker_shm_insn(
    pool: &mut DBConnection,
    broker: &BrokerSHMInsn,
) -> Result<i64, rusqlite::Error> {
    let tx = pool.transaction()?;

    // Insert TBInsnInfo
    let insn_info_id = insert_tb_insn_info(&tx, None, &broker.insn_info)?;

    // Insert broker_shm_exec
    tx.execute(
        r#"
        INSERT INTO broker_shm (init_mask, shm_type)
        VALUES (?, ?)
        "#,
        params![broker.init_mask, SHMTYPE_INSN],
    )?;

    let broker_id = tx.last_insert_rowid();

    tx.execute(
        r#"
           INSERT INTO broker_shm_insn (id, insn_info_id) 
           VALUES (?, ?)
        "#,
        params![broker_id, insn_info_id],
    )?;

    // Insert CPUs and registers
    for (idx, cpu) in broker.cpus.iter().enumerate() {
        if (broker.init_mask & (1 << idx)) != 0 {
            let cpu_id = insert_shm_cpu(&tx, broker_id, cpu)?;
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
    broker_id: i64,
    cpu: &SHMCPU,
) -> Result<i64, rusqlite::Error> {
    tx.execute(
        "INSERT INTO shm_cpu (idx, broker_shm_id) VALUES (?, ?)",
        params![cpu.idx, broker_id],
    )?;

    Ok(tx.last_insert_rowid())
}

fn insert_shm_registers(
    tx: &rusqlite::Transaction<'_>,
    cpu_id: i64,
    registers: &[SHMRegister],
) -> Result<(), rusqlite::Error> {
    for reg in registers {
        tx.execute(
            "INSERT INTO shm_register (cpu_id, size, data, name) VALUES (?, ?, ?, ?)",
            params![cpu_id, reg.size, reg.data_slice(), reg.name.as_str()],
        )?;
    }
    Ok(())
}

fn select_shm_registers(
    tx: &rusqlite::Transaction<'_>,
    cpu_id: i64,
) -> Result<Vec<SHMRegister>, rusqlite::Error> {
    let mut stmt = tx.prepare("SELECT name, size, data FROM shm_register WHERE cpu_id = ?")?;
    stmt.query_map([cpu_id], |row| {
        let name: String = row.get(0)?;
        let name = name.into();
        let size = row.get(1)?;
        let data = row.get(2)?;
        Ok(SHMRegister::new(size, data, name))
    })?
    .collect()
}
//
// fn select_shm_cpu(
//     tx: &rusqlite::Transaction<'_>,
// )
