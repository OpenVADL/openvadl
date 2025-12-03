use std::str::FromStr;

use anyhow::{Result, bail};
use clap::Parser;
use figment::{
    Figment,
    providers::{Format, Toml},
};
use tracing::{Level, info};

use cosim_lib::{config::Config, cosim::Broker, diff::Report};

#[cfg(feature = "sqlite-tracing")]
use cosim_lib::db::setup_database;

#[cfg(feature = "sqlite-tracing")]
use cosim_lib::trace::connect;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
pub struct Cli {
    /// Path to the (toml) config file
    #[arg(short, long, value_name="FILE", default_value_t = default_config_file())]
    pub config: String,

    /// Defines where the test-executable is passed to when starting the QEMU-client
    /// If not set, the values from the config-file will be taken
    /// If only one value is set then all clients will receive this path
    /// If multiple values are set (--test-exec foo --test-exec bar) then each path will be
    /// assigned to each client with the same order as in the config-file.
    #[arg(short, long, value_name = "FILE")]
    pub test_exec: Option<Vec<String>>,

    /// If set, writes the test-result to the given output-file.
    /// Overrides the value that is set in the config file at testing.protocol.out.file
    #[arg(short, long, value_name = "FILE")]
    pub output_file: Option<String>
}

fn default_config_file() -> String {
    "./config.toml".into()
}

fn main() -> Result<()> {
    let cli = Cli::parse();

    let mut config: Config = Figment::new().merge(Toml::file(cli.config)).extract()?;

    config.qemu.set_inverse_reg_map();

    if let Some(test_exec) = cli.test_exec {
        match &test_exec[..] {
            [test_exec] => {
                for client in &mut config.qemu.clients {
                    client.test_exec = test_exec.clone();
                }
            }
            test_execs => {
                if test_execs.len() != config.qemu.clients.len() {
                    bail!(
                        "Multiple test-execs were provided using --test-exec, but the provided amount ({}) does not match the amount of clients in the config-file ({})",
                        test_execs.len(),
                        config.qemu.clients.len()
                    );
                }

                for (i, client) in config.qemu.clients.iter_mut().enumerate() {
                    client.test_exec = test_execs[i].clone();
                }
            }
        }
    }

    if cli.output_file.is_some() {
        config.testing.protocol.out.file = cli.output_file;
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

    #[cfg(feature = "sqlite-tracing")]
    if config.tracing.mode.enabled() && config.tracing.clear_on_rerun {
        use anyhow::Context;
        let mut conn = connect(&config)?;
        setup_database(&mut conn).context("failed to setup database")?;
    }

    run(config)
}

fn run(config: Config) -> Result<()> {
    let mut broker = Broker::create(&config)?;
    let report_data = broker.run(&config)?;
    let passed = report_data.passed;

    let report = match &config.testing.protocol.out.verbosity {
        cosim_lib::config::OutVerbosity::Full => serde_yaml::to_string(&report_data)?,
        cosim_lib::config::OutVerbosity::Short => {
            let mut buf = String::new();
            if report_data.passed {
                buf.push_str("Cosimulation passed!");
                for client in broker.clients() {
                    let s = format!(
                        "\n\t\"{}\" executed {} steps",
                        client.name.clone().unwrap_or(client.id.to_string()),
                        client.run_count
                    );
                    buf.push_str(&s);
                }
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

    broker.finish(passed, &config)
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
        .min_by_key(|insns| insns.len());

    if let Some(min_insns) = min_insns {
        for insn in min_insns {
            let pc = insn.pc;
            let disas = &insn.disas;
            let insn_data = &insn.insn_data;
            buf.push_str(&format!("- (pc={pc}): {disas} ({insn_data})\n"));
        }
    }
}
