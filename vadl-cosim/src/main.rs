use std::{fs::File, io::Write, str::FromStr};

use anyhow::Result;
use clap::Parser;
use figment::{
    Figment,
    providers::{Format, Toml},
};
use tracing::{info, Level};

use crate::{cli::Cli, config::Config, cosim::Broker};

pub mod config;
pub mod cosim;
pub mod diff;
pub mod ipc;
pub mod cli;

fn main() -> Result<()> {
    let cli = Cli::parse();

    let mut config: Config = Figment::new()
        .merge(Toml::file(cli.config))
        .extract()?;

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

    let traces = &broker
        .traces
        .read()
        .unwrap()
        .deque;

    let res = serde_json::to_string_pretty(traces)?;

    let mut f = File::create("./trace.json")?;
    f.write_all(res.as_bytes())?;

    Ok(())
}
