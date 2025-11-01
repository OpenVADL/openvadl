use anyhow::bail;
use rusqlite::{Transaction, params};

use crate::{
    cosim::DBConnection,
    db::dbstructs::{
        BrokerData, BrokerInsn, BrokerTB, CPU, Client, ClientEntry, Register, TBInfo, TBInsnInfo,
    },
    ipc::{cstructs, qemu},
};

pub mod dbstructs;

const CREATES_SQL: &str = include_str!("../../res/creates.sql");
const DROPS_SQL: &str = include_str!("../../res/drops.sql");

pub fn setup_database(pool: &mut DBConnection) -> Result<(), rusqlite::Error> {
    let tx = pool.transaction()?;
    tx.execute_batch(&format!("{DROPS_SQL}; {CREATES_SQL}"))?;
    tx.commit()?;
    Ok(())
}

const TYPE_TB: u8 = 0;
const TYPE_INSN: u8 = 1;

pub struct CosimRunInfo {
    pub run_id: i64,
    pub client_ids: Vec<i64>,
}

pub fn insert_new_cosimulation_run(
    pool: &mut DBConnection,
    clients: &Vec<qemu::Client>,
) -> Result<CosimRunInfo, anyhow::Error> {
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
) -> Result<(), anyhow::Error> {
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
    run_count: u64,
) -> Result<(), anyhow::Error> {
    pool.execute(
        r#"INSERT INTO client_entry(client_id, broker_id, run_count) VALUES (?, ?, ?)"#,
        params![client_id, broker_id, run_count],
    )?;
    Ok(())
}

pub fn insert_cosim_run_client(
    tx: &rusqlite::Transaction<'_>,
    run_id: i64,
    client_id: i64,
) -> Result<(), anyhow::Error> {
    tx.execute(
        r#"INSERT INTO cosimulation_run_clients(run_id, client_id) VALUES (?, ?)"#,
        params![run_id, client_id],
    )?;
    Ok(())
}

pub fn insert_broker_shm_tb(
    pool: &mut DBConnection,
    broker: &cstructs::BrokerSHMTB,
) -> Result<i64, anyhow::Error> {
    let tx = pool.transaction()?;

    let tb_info_id = insert_tb_info(&tx, &broker.tb_info)?;

    tx.execute(
        r#"
        INSERT INTO broker (init_mask, type)
        VALUES (?, ?)
        "#,
        params![broker.init_mask, TYPE_TB],
    )?;

    let broker_id = tx.last_insert_rowid();

    tx.execute(
        r#"
           INSERT INTO broker_tb (id, tb_info_id) 
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
    broker: &cstructs::BrokerSHMInsn,
) -> Result<i64, anyhow::Error> {
    let tx = pool.transaction()?;

    let insn_info_id = insert_tb_insn_info(&tx, None, &broker.insn_info)?;

    tx.execute(
        r#"
        INSERT INTO broker (init_mask, type)
        VALUES (?, ?)
        "#,
        params![broker.init_mask, TYPE_INSN],
    )?;

    let broker_id = tx.last_insert_rowid();

    tx.execute(
        r#"
           INSERT INTO broker_insn (id, insn_info_id) 
           VALUES (?, ?)
        "#,
        params![broker_id, insn_info_id],
    )?;

    // Insert CPUs and registers
    for (idx, cpu) in broker.cpus().expect("insn-exec").iter().enumerate() {
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
    tb_info: &cstructs::TBInfo,
) -> Result<i64, anyhow::Error> {
    let pc = tb_info.pc as i64;
    tx.execute("INSERT INTO tb_info (pc) VALUES (?)", params![pc])?;

    let tb_info_id = tx.last_insert_rowid();

    for insn in tb_info.insns_info_slice() {
        insert_tb_insn_info(tx, Some(tb_info_id), insn)?;
    }

    Ok(tb_info_id)
}

fn insert_tb_insn_info(
    tx: &rusqlite::Transaction<'_>,
    tb_info_id: Option<i64>,
    insn: &cstructs::TBInsnInfo,
) -> Result<i64, anyhow::Error> {
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
    cpu: &cstructs::SHMCPU,
) -> Result<i64, anyhow::Error> {
    tx.execute(
        "INSERT INTO cpu (idx, broker_id) VALUES (?, ?)",
        params![cpu.idx, broker_id],
    )?;

    Ok(tx.last_insert_rowid())
}

fn insert_shm_registers(
    tx: &rusqlite::Transaction<'_>,
    cpu_id: i64,
    registers: &[cstructs::SHMRegister],
) -> Result<(), anyhow::Error> {
    for reg in registers {
        tx.execute(
            "INSERT INTO register (cpu_id, size, data, name) VALUES (?, ?, ?, ?)",
            params![cpu_id, reg.size, reg.data_slice(), reg.name.as_str()],
        )?;
    }
    Ok(())
}

pub fn select_registers(
    tx: &rusqlite::Transaction<'_>,
    cpu_id: i64,
) -> Result<Vec<Register>, anyhow::Error> {
    let mut stmt = tx.prepare("SELECT name, size, data FROM register WHERE cpu_id = ?")?;
    stmt.query_map([cpu_id], |row| {
        let name = row.get(0)?;
        let size = row.get(1)?;
        let data = row.get(2)?;
        Ok(Register::new(size, data, name))
    })?
    .collect::<Result<_, _>>()
    .map_err(anyhow::Error::from)
}

pub fn select_cpu(tx: &rusqlite::Transaction<'_>, cpu_id: i64) -> Result<CPU, anyhow::Error> {
    let mut stmt = tx.prepare(r#"SELECT idx FROM cpu WHERE id = ?"#)?;
    let cpu_idx = stmt.query_one(params![cpu_id], |row| row.get(0))?;

    let registers = select_registers(tx, cpu_id)?;

    let cpu = CPU::new(cpu_idx, registers);
    Ok(cpu)
}

pub fn select_broker_cpus(tx: &Transaction<'_>, broker_id: i32) -> Result<Vec<CPU>, anyhow::Error> {
    let mut cpus = vec![];
    let mut stmt = tx.prepare(r#"SELECT c.id FROM cpu c WHERE c.broker_id = ?"#)?;
    let cpu_ids = stmt
        .query_map(params![broker_id], |row| row.get(0))?
        .collect::<Result<Vec<_>, _>>()?;

    for cpu_id in cpu_ids {
        let cpu = select_cpu(tx, cpu_id)?;
        cpus.push(cpu);
    }

    Ok(cpus)
}

fn map_tb_insn_info_row(row: &rusqlite::Row<'_>) -> Result<TBInsnInfo, rusqlite::Error> {
    let pc = row.get(0)?;
    let size = row.get(1)?;
    let symbol: String = row.get(2)?;
    let hwaddr: String = row.get(3)?;
    let disas: String = row.get(4)?;
    let data_buffer = row.get(6)?;

    Ok(TBInsnInfo::new(
        pc,
        size,
        symbol,
        hwaddr,
        disas,
        data_buffer,
    ))
}

pub fn select_tb_insn_info_by_id(
    tx: &Transaction<'_>,
    tb_insn_info_id: i64,
) -> Result<TBInsnInfo, anyhow::Error> {
    let mut stmt = tx.prepare(
        r#"
        SELECT
            pc,
            size,
            symbol,
            hwaddr,
            disas,
            data_size,
            data
        FROM tb_insn_info
        WHERE id = ?
    "#,
    )?;
    let tb_insn_info = stmt.query_one(params![tb_insn_info_id], map_tb_insn_info_row)?;

    Ok(tb_insn_info)
}

pub fn select_tb_info_by_id(
    tx: &Transaction<'_>,
    tb_info_id: i64,
) -> Result<TBInfo, anyhow::Error> {
    let mut tb_info_stmt = tx.prepare(r#"SELECT pc FROM tb_info WHERE id = ?"#)?;
    let tb_info_pc = tb_info_stmt.query_one(params![tb_info_id], |row| row.get(0))?;

    let mut tb_insn_info_stmt = tx.prepare(
        r#"
        SELECT
            pc,
            size,
            symbol,
            hwaddr,
            disas,
            data_size,
            data
        FROM tb_insn_info
        WHERE tb_info_id = ?
    "#,
    )?;

    let tb_insn_infos = tb_insn_info_stmt
        .query_map(params![tb_info_id], map_tb_insn_info_row)?
        .collect::<Result<Vec<_>, _>>()?;

    let tb_info = TBInfo::new(tb_info_pc, tb_insn_infos);
    Ok(tb_info)
}

pub fn select_broker_tb(tx: &Transaction<'_>, broker_id: i32) -> Result<BrokerTB, anyhow::Error> {
    let (init_mask, tb_info_id) = tx.query_one(
        r#"
            SELECT init_mask, tb_info_id
            FROM broker b1
            INNER JOIN broker_tb b2 ON b1.id = b2.id
            WHERE b1.id = ?
        "#,
        params![broker_id],
        |row| Ok((row.get(0)?, row.get(1)?)),
    )?;

    let cpus = select_broker_cpus(tx, broker_id)?;
    let tb_info = select_tb_info_by_id(tx, tb_info_id)?;

    Ok(BrokerTB::new(init_mask, cpus, tb_info))
}

pub fn select_broker_insn(
    tx: &Transaction<'_>,
    broker_id: i32,
) -> Result<BrokerInsn, anyhow::Error> {
    let (init_mask, insn_info_id) = tx.query_one(
        r#"
            SELECT init_mask, insn_info_id
            FROM broker b1
            INNER JOIN broker_insn b2 ON b1.id = b2.id
            WHERE b1.id = ?
        "#,
        params![broker_id],
        |row| Ok((row.get(0)?, row.get(1)?)),
    )?;

    let cpus = select_broker_cpus(tx, broker_id)?;
    let insn_info = select_tb_insn_info_by_id(tx, insn_info_id)?;

    Ok(BrokerInsn::new(init_mask, cpus, insn_info))
}

pub fn select_broker(tx: &Transaction<'_>, broker_id: i32) -> Result<BrokerData, anyhow::Error> {
    let broker_type = tx.query_one(
        r#"SELECT type FROM broker WHERE id = ?"#,
        params![broker_id],
        |row| row.get(0),
    )?;

    let broker_data = match broker_type {
        TYPE_TB => select_broker_tb(tx, broker_id)?.into(),
        TYPE_INSN => select_broker_insn(tx, broker_id)?.into(),
        _ => bail!("illegal type found in database when selecting broker-data: {broker_type}"),
    };

    Ok(broker_data)
}

pub fn select_client_entry_with_run_count(
    tx: &Transaction<'_>,
    client_id: i32,
    run_count: u64,
) -> Result<ClientEntry, anyhow::Error> {
    let (broker_id, client_id, client_name) = tx.query_one(
        r#"
            SELECT ce.broker_id, c.id, c.name 
            FROM client_entry ce 
            INNER JOIN client c ON ce.client_id = c.id 
            WHERE ce.client_id = ? AND ce.run_count = ?
        "#,
        params![client_id, run_count],
        |row| {
            let broker_id = row.get(0)?;
            let client_id = row.get(1)?;
            let client_name = row.get(2)?;
            Ok((broker_id, client_id, client_name))
        },
    )?;

    let broker = select_broker(tx, broker_id)?;

    let client = Client::new(client_id, client_name);

    Ok(ClientEntry::new(client, broker, run_count))
}

pub fn select_cosim_run_entries_at_run_count(
    pool: &mut DBConnection,
    client_ids: &[i32],
    run_count: u64,
) -> Result<Vec<ClientEntry>, anyhow::Error> {
    client_ids
        .iter()
        .map(|client_id| {
            let tx = pool.transaction()?;
            let entry = select_client_entry_with_run_count(&tx, *client_id, run_count)?;
            tx.commit()?;
            Ok(entry)
        })
        .collect::<Result<Vec<_>, anyhow::Error>>()
}

pub fn select_cosim_run_entries_length(
    pool: &mut DBConnection,
    run_id: i64,
) -> Result<u64, anyhow::Error> {
    let mut stmt = pool.prepare(
        r#"
        SELECT MAX(run_count)
        FROM cosimulation_run_clients crc 
        INNER JOIN client_entry ce ON crc.client_id = ce.client_id
        WHERE crc.run_id = ?;
    "#,
    )?;

    let max_run_count = stmt.query_row(params![run_id], |row| row.get(0))?;

    Ok(max_run_count)
}

pub fn select_cosim_run_clients(
    pool: &mut DBConnection,
    run_id: i64,
) -> Result<Vec<Client>, anyhow::Error> {
    let mut stmt = pool.prepare(
        r#"
        SELECT c.id, c.name 
        FROM cosimulation_run_clients crc 
        INNER JOIN client c ON crc.client_id = c.id 
        WHERE crc.run_id = ?
    "#,
    )?;

    let clients = stmt
        .query_map(params![run_id], |row| {
            let client_id = row.get(0)?;
            let client_name = row.get(1)?;
            Ok(Client::new(client_id, client_name))
        })?
        .collect::<Result<Vec<_>, _>>()?;

    Ok(clients)
}
