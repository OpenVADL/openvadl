use std::path::Path;

use anyhow::{Context, Result};

use crate::{
    config::Config,
    ipc::cstructs::{BrokerSHMInsn, BrokerSHMTB},
    trace::db::{insert_broker_shm_insn, insert_broker_shm_tb, insert_client_entry},
};

use rusqlite::{Connection, OpenFlags};

pub mod db;

#[derive(Debug)]
pub struct TraceEntryData {
    /// The id of the `client` table in the database
    client_db_id: i64,
    broker_data: TraceBrokerData,
}

impl TraceEntryData {
    pub fn new(client_db_id: i64, broker_data: TraceBrokerData) -> Self {
        Self {
            client_db_id,
            broker_data,
        }
    }
}

#[derive(Debug)]
pub enum TraceBrokerData {
    TB(Box<BrokerSHMTB>),
    Insn(Box<BrokerSHMInsn>),
}

pub fn connect(config: &Config) -> Result<Connection> {
    let connect_flags = OpenFlags::SQLITE_OPEN_READ_WRITE
        | OpenFlags::SQLITE_OPEN_NOFOLLOW
        | OpenFlags::SQLITE_OPEN_CREATE
        | OpenFlags::SQLITE_OPEN_NO_MUTEX;
    let path = Path::new(&config.tracing.dir).join(&config.tracing.file);
    Connection::open_with_flags(path, connect_flags)
        .context("failed to open sqlite connection for tracing")
}

pub fn store_trace(trace: TraceEntryData, connection: &mut Connection) -> Result<()> {
    let broker_id = match trace.broker_data {
        TraceBrokerData::TB(broker_shmtb) => insert_broker_shm_tb(connection, &broker_shmtb),
        TraceBrokerData::Insn(broker_shmexec) => {
            insert_broker_shm_insn(connection, &broker_shmexec)
        }
    }?;

    insert_client_entry(connection, trace.client_db_id, broker_id)?;

    Ok(())
}

pub fn get_client_trace(client: &crate::ipc::qemu::Client, config: &Config) -> TraceBrokerData {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            let insn = Box::new(client.shms.current().get_insn().clone());
            TraceBrokerData::Insn(insn)
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            let tb = Box::new(client.shms.current().get_tb().clone());
            TraceBrokerData::TB(tb)
        }
    }
}

pub type TraceStore = Vec<TraceEntryData>;
pub fn trace_collect(
    clients: &[crate::ipc::qemu::Client],
    client_ids: &[i64],
    config: &crate::config::Config,
    store: &mut TraceStore,
) {
    assert!(
        clients.len() == client_ids.len(),
        "illegal call to trace_collect with different client-lens: {} != {}",
        clients.len(),
        client_ids.len()
    );
    clients
        .iter()
        .map(|c| get_client_trace(c, config))
        .enumerate()
        .map(|(idx, broker_data)| TraceEntryData {
            client_db_id: client_ids[idx],
            broker_data,
        })
        .for_each(|t| store.push(t));
}
