use std::{mem::ManuallyDrop, path::Path};

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
    let connect_flags = OpenFlags::SQLITE_OPEN_READ_WRITE | OpenFlags::SQLITE_OPEN_NOFOLLOW;
    let path = Path::new(&config.tracing.dir).join(&config.tracing.file);
    Connection::open_with_flags(path, connect_flags)
        .context("failed to open sqlite connection for tracing")
}

pub fn trace_threaded(clients: &Vec<crate::ipc::qemu::Client>, config: &crate::config::Config) -> Result<()> {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            for c in clients {
                let exec = unsafe { &c.shm.get().data.shm_exec };
                let exec = exec.clone();
                let c = config.clone();
                rayon::spawn(move || {
                    let conn = connect(&c).unwrap();
                    let _ = insert_broker_shm_exec(&conn, &exec);
                });
            }
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            for c in clients {
                let tb = unsafe { &c.shm.get().data.shm_tb };
                let tb = tb.clone();
                let c = config.clone();
                rayon::spawn(move || {
                    let conn = connect(&c).unwrap();
                    let _ = insert_broker_shm_tb(&conn, &tb);
                });
            }
        }
    };

    Ok(())
}

pub fn trace_sync(
    clients: &Vec<crate::ipc::qemu::Client>,
    config: &crate::config::Config,
) -> Result<()> {
    let conn = connect(config)?;
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            for c in clients {
                let exec = unsafe { &c.shm.get().data.shm_exec };
                let exec = exec.clone();
                insert_broker_shm_exec(&conn, &exec)?;
            }
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            for c in clients {
                let tb = unsafe { &c.shm.get().data.shm_tb };
                let tb = tb.clone();
                insert_broker_shm_tb(&conn, &tb)?;
            }
        }
    };

    Ok(())
}

pub type TraceStore = Vec<TraceData>;
pub fn trace_collect(
    clients: &Vec<crate::ipc::qemu::Client>,
    config: &crate::config::Config,
    store: &mut TraceStore,
) {
    match config.testing.protocol.layer {
        crate::config::ProtocolLayer::Insn => {
            for c in clients {
                let exec = unsafe { &c.shm.get().data.shm_exec };
                let exec = exec.clone();
                let exec = ManuallyDrop::into_inner(exec);
                let exec = Box::new(exec);
                store.push(TraceData::Exec(exec));
            }
        }
        crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => {
            for c in clients {
                let tb = unsafe { &c.shm.get().data.shm_tb };
                let tb = tb.clone();
                let tb = ManuallyDrop::into_inner(tb);
                let tb = Box::new(tb);
                store.push(TraceData::TB(tb));
            }
        }
    };
}
