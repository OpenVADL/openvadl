use std::str::FromStr;

use anyhow::Result;
use clap::Parser;
use figment::{
    Figment,
    providers::{Format, Toml},
};
use tracing::{Level, info};

use cosim_lib::{
    cli::Cli,
    config::Config,
    cosim::Broker, trace::{connect, db::setup_database},
};


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

    if config.tracing.clear_on_rerun {
        let conn = connect(&config)?;
        setup_database(&conn)?;
    }

    let mut broker = Broker::create(&config)?;
    let report = broker.run(&config)?;

    dbg!(report);

    broker.finish(&config)?;


    Ok(())
}

