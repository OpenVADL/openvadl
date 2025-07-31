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
    cosim::Broker,
    diff::Report,
    trace::{connect, db::setup_database},
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
            .with_writer(std::io::stderr)
            .init();
    }

    if config.dev.dry_run {
        info!(?config, "Dry-Run.");
        return Ok(());
    }

    if config.tracing.clear_on_rerun {
        let mut conn = connect(&config)?;
        setup_database(&mut conn)?;
    }

    let mut broker = Broker::create(&config)?;
    let report_data = broker.run(&config)?;
    let passed = report_data.passed;

    let report = match &config.testing.protocol.out.verbosity {
        cosim_lib::config::OutVerbosity::Full => serde_json::to_string_pretty(&report_data)?,
        cosim_lib::config::OutVerbosity::Short => {
            let mut buf = String::new();
            if report_data.passed {
                buf.push_str("Cosimulation passed!");
            } else {
                add_plain_report_summary(&mut buf, &report_data);
            }
            buf
        }
    };

    match config.testing.protocol.out.file {
        Some(ref file) => {
            std::fs::write(file, report)?;
        }
        None => println!("{report}"),
    }

    broker.finish(passed, &config)?;

    Ok(())
}

fn add_plain_report_summary(buf: &mut String, report: &Report) {
    buf.push_str("Cosimulation failed!\n");
    let pc = report.diff_context[0].after_state.pc;
    buf.push_str(&format!("Failure at pc = 0x{pc:02X?} ({pc})\n\n"));

    buf.push_str("The following divergences were found:\n");

    for diff in &report.diffs {
        let desc = &diff.description;
        buf.push_str(&format!("- \"{desc}\":\n"));
        for i in 0..diff.values.len() {
            let v = &diff.values[i];
            let ctx = &report.diff_context[i];
            let name = ctx.client_name.clone().unwrap_or(ctx.client_id.to_string());

            buf.push_str(&format!("\t- In \"{name}\" the value is \"{v}\"\n"));
        }
    }

    buf.push_str("\nThe divergence occurred after the following instructions were executed:\n");

    let min_insns = report
        .diff_context
        .iter()
        .map(|ctx| &ctx.error_instruction.0)
        .min_by_key(|insns| insns.len())
        .unwrap();

    for insn in min_insns {
        let pc = insn.pc;
        let disas = &insn.disas;
        let insn_data = &insn.insn_data;
        buf.push_str(&format!("- (pc={pc}): {disas} ({insn_data})\n"));
    }
}
