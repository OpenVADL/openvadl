use std::{
    mem::ManuallyDrop,
    path::Path,
};

use anyhow::{Context, Result};
use serde::Serialize;

use crate::{
    config::Config,
    ipc::cstructs::{BrokerSHMExec, BrokerSHMTB},
    trace::db::{insert_broker_shm_exec, insert_broker_shm_tb},
};

use rusqlite::{Connection, OpenFlags};

pub mod db;

#[derive(Debug, Serialize)]
pub enum TraceData {
    TB(Box<BrokerSHMTB>),
    Exec(Box<BrokerSHMExec>),
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

pub fn store_trace(
    trace: TraceData,
    connection: &Connection,
) -> Result<()> {
    match trace {
        TraceData::TB(broker_shmtb) => {
            insert_broker_shm_tb(connection, &broker_shmtb)?;
        },
        TraceData::Exec(broker_shmexec) => {
            insert_broker_shm_exec(connection, &broker_shmexec)?;
        },
    };

    Ok(())
}

pub fn get_client_trace(client: &crate::ipc::qemu::Client, config: &Config) -> TraceData {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            let exec = unsafe { &client.shm.read().shm_exec };
            let exec = exec.clone();
            let exec = ManuallyDrop::into_inner(exec);
            let exec = Box::new(exec);
            TraceData::Exec(exec)
        }
        crate::config::ProtocolLayer::TB |
        crate::config::ProtocolLayer::TBStrict => {
            let tb = unsafe { &client.shm.read().shm_tb };
            let tb = tb.clone();
            let tb = ManuallyDrop::into_inner(tb);
            let tb = Box::new(tb);
            TraceData::TB(tb)
        },
    }
}

pub type TraceStore = Vec<TraceData>;
pub fn trace_collect(
    clients: &[crate::ipc::qemu::Client],
    config: &crate::config::Config,
    store: &mut TraceStore,
) {
    clients.iter()
        .map(|c| get_client_trace(c, config))
        .for_each(|t| store.push(t));
}