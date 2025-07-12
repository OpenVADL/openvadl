use std::str::FromStr;

use anyhow::Result;
use clap::Parser;
use figment::{
    Figment,
    providers::{Format, Toml},
};
use tracing::{Level, info};

use crate::{
    cli::Cli,
    config::Config,
    cosim::Broker,
    trace::{
        connect,
        db::{insert_broker_shm_exec, insert_broker_shm_tb},
    },
};

pub mod cli;
pub mod config;
pub mod cosim;
pub mod diff;
pub mod ipc;
pub mod trace;

fn main() -> Result<()> {
    let cli = Cli::parse();

    let mut config: Config = Figment::new().merge(Toml::file(cli.config)).extract()?;

    config.qemu.set_inverse_reg_map();

    if let Some(test_exec) = cli.test_exec {
        config.testing.test_exec = test_exec;
    }

    if config.logging.enable {
        let level = Level::from_str(&config.logging.level)?;
        tracing_subscriber::fmt()
            .pretty()
            .with_max_level(level)
            .init();
    }

    if config.dev.dry_run {
        info!(?config, "Dry-Run.");
        return Ok(());
    }

    let mut broker = Broker::create(&config)?;
    let report = broker.run(&config)?;

    dbg!(report);

    if config.tracing.mode == config::TracingMode::Collect {
        for entry in broker.trace_store {
            let c = config.clone();
            rayon::spawn(move || {
                let conn = connect(&c).unwrap();
                match entry {
                    trace::TraceData::TB(broker_shmtb) => {
                        let _ = insert_broker_shm_tb(&conn, &broker_shmtb);
                    },
                    trace::TraceData::Exec(broker_shmexec) => {
                        let _ = insert_broker_shm_exec(&conn, &broker_shmexec);
                    },
                }
            });
        }
    }

    Ok(())
}
