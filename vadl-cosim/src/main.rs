use std::{fs::File, io::Write};

use anyhow::Result;
use figment::{
    Figment,
    providers::{Format, Toml},
};
use tracing::Level;

use crate::{config::Config, cosim::Broker};

pub mod config;
pub mod cosim;
pub mod diff;
pub mod ipc;

fn main() -> Result<()> {
    tracing_subscriber::fmt()
        .pretty()
        .with_max_level(Level::DEBUG)
        .init();

    let config: Config = Figment::new()
        .merge(Toml::file("./open-vadl/vadl-cosim/config.toml"))
        .extract()?;

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
