use std::path::Path;

use anyhow::{anyhow, Context, Result};

use crate::{
    config::Config,
    db::{insert_broker_shm_insn, insert_broker_shm_tb, insert_client_entry},
    ipc::cstructs::{BrokerSHMInsn, BrokerSHMTB},
};

use rusqlite::{Connection, OpenFlags};

#[derive(Debug)]
pub struct TraceEntryData {
    /// The id of the `client` table in the database
    client_db_id: i64,
    run_count: u64,
    broker_data: TraceBrokerData,
}

impl TraceEntryData {
    pub fn new(client_db_id: i64, run_count: u64, broker_data: TraceBrokerData) -> Self {
        Self {
            client_db_id,
            run_count,
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

    insert_client_entry(connection, trace.client_db_id, broker_id, trace.run_count)?;

    Ok(())
}

pub fn get_client_trace(
    client: &mut crate::ipc::qemu::Client,
    config: &Config,
) -> anyhow::Result<TraceBrokerData> {
    let data = match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            let insn = Box::new(
                client
                    .shm
                    .read_buffer()?
                    .ok_or(anyhow!("expected to be able to read ringbuffer"))?
                    .as_insn()
                    .clone(),
            );
            TraceBrokerData::Insn(insn)
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            let tb = Box::new(
                client
                    .shm
                    .read_buffer()?
                    .ok_or(anyhow!("expected to be able to read ringbuffer"))?
                    .as_tb()
                    .clone(),
            );
            TraceBrokerData::TB(tb)
        }
    };
    Ok(data)
}

pub type TraceStore = Vec<TraceEntryData>;
pub fn trace_collect(
    clients: &mut [crate::ipc::qemu::Client],
    client_ids: &[i64],
    config: &crate::config::Config,
    store: &mut TraceStore,
) -> anyhow::Result<()> {
    assert!(
        clients.len() == client_ids.len(),
        "illegal call to trace_collect with different client-lens: {} != {}",
        clients.len(),
        client_ids.len(),
    );

    for (idx, client) in clients.iter_mut().enumerate() {
        let broker_data = get_client_trace(client, config)?;
        let entry = TraceEntryData::new(client_ids[idx], client.run_count, broker_data);
        store.push(entry);
    }

    Ok(())
}
