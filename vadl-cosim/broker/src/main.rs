use std::{
    ffi::OsStr,
    fs::{self, File},
    path::Path,
    str::FromStr,
};

use clap::Parser;
use color_eyre::{
    Result,
    eyre::{Context, bail},
};
use figment::{
    Figment,
    providers::{Format, Toml},
};
use object::{Object, ObjectSymbol};
use tracing::{Level, info};

use cosim_lib::{
    config::{self, Config},
    cosim::Broker,
    diff::Report,
};

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
    pub output_file: Option<String>,

    /// If set, instructs the cosim-broker to stop cosimulation at either the specified address, or
    /// at the address of the given label.
    #[arg(long, value_name = "<ADDR>|<LABEL>")]
    pub exit_on_exec: Option<String>,

    /// If set, instructs the cosim-broker to stop cosimulation at whenever a memory-write occurs
    /// at the given address (hardcoded or by symbol). This can further be filtered to only exit if
    /// a certain value (hardcoded or by symbol) gets written to the address.
    #[arg(long, value_name = "(<ADDR>|<SYMBOL>)[,(<VALUE>)]")]
    pub exit_on_write: Option<String>,
}

fn default_config_file() -> String {
    "./config.toml".into()
}

fn main() -> Result<()> {
    color_eyre::install()?;

    let cli = Cli::parse();

    let config_path = Path::new(&cli.config);
    if !config_path.exists() {
        bail!("The provided path \"{}\" does not exist", cli.config);
    }

    if !config_path.is_file() {
        bail!("The provided path \"{}\" is not a file", cli.config);
    }

    let mut config: Config = Figment::new().merge(Toml::file(cli.config)).extract()?;

    config.qemu.set_defined_registers_map();

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

    let obj_test_file_name = &config.for_client(0).test_exec.clone();
    let obj_test_file_data = fs::read(obj_test_file_name)?;
    let obj_test_file = object::File::parse(&*obj_test_file_data)?;

    if let Some(exit_on_exec) = &config.testing.exit_condition.on_label {
        let addr = parse_exit_on_exec_argument(exit_on_exec, &obj_test_file, obj_test_file_name)?;
        config.testing.exit_condition.on_address = Some(addr);
    }

    if let Some(exit_on_exec) = cli.exit_on_exec {
        let addr = parse_exit_on_exec_argument(&exit_on_exec, &obj_test_file, obj_test_file_name)?;
        config.testing.exit_condition.on_address = Some(addr);
    }

    let mem_write = &mut config.testing.exit_condition.on_mem_write;
    if let Some(ref mem_write_label) = mem_write.on_label {
        let addr = parse_address_or_symbol(mem_write_label, &obj_test_file, obj_test_file_name)?;
        mem_write.on_address = Some(addr as u64);
    }

    if let Some(exit_on_write) = cli.exit_on_write {
        let (addr, write_value) =
            parse_exit_on_write_argument(&exit_on_write, &obj_test_file, obj_test_file_name)?;
        let on_mem_write = &mut config.testing.exit_condition.on_mem_write;
        on_mem_write.on_address = Some(addr);
        on_mem_write.with_constant_value = write_value;
    }

    if cli.output_file.is_some() {
        config.testing.protocol.out.file = cli.output_file;
    }

    if config.dev.dry_run {
        info!(?config, "Dry-Run.");
        return Ok(());
    }

    if config.logging.enable {
        if !config.logging.dir.is_dir() {
            fs::create_dir_all(&config.logging.dir)?;
        }

        // ensure the logfile has an appropriate extension
        if let Some(ref mut logfile) = config.logging.file {
            logfile.set_extension(config::COSIM_LOG_EXTENSION);
        }

        if config.logging.clear_on_rerun {
            // NOTE: This deliberately does not delete the whole directory in case the path is
            // wrong!
            for entry in fs::read_dir(&config.logging.dir)? {
                let path = entry?.path();
                if let Some(extension) = path.extension()
                    && extension == OsStr::new(config::COSIM_LOG_EXTENSION)
                {
                    fs::remove_file(path)?;
                }
            }
        }

        let level = Level::from_str(&config.logging.level)?;

        if let Some(ref logfile) = config.logging.file {
            let logfile = config.logging.dir.join(logfile);
            let logfile = File::create(logfile)?;
            tracing_subscriber::fmt()
                .pretty()
                .with_ansi(false)
                .with_writer(logfile)
                .with_max_level(level)
                .init();
        } else {
            tracing_subscriber::fmt()
                .pretty()
                .with_writer(std::io::stderr)
                .with_max_level(level)
                .init();
        }
    }

    #[cfg(feature = "sqlite-tracing")]
    if config.tracing.mode.enabled() && config.tracing.clear_on_rerun {
        use color_eyre::eyre::WrapErr;
        let mut conn = connect(&config)?;
        setup_database(&mut conn).wrap_err("failed to setup database")?;
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
    let pc = match &report.diff_context[0].after_state {
        Some(s) => s.pc,
        None => report.diff_context[0].before_state.pc,
    };
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

fn parse_exit_on_exec_argument(
    exit_on_exec: &str,
    elf: &object::File<'_>,
    elf_name: &str,
) -> Result<u64> {
    let addr = parse_address_or_symbol(exit_on_exec, elf, elf_name)?;
    Ok(addr as u64)
}

fn parse_exit_on_write_argument(
    exit_on_write: &str,
    elf: &object::File<'_>,
    elf_name: &str,
) -> Result<(u64, Option<u128>)> {
    let exit_on_write = exit_on_write.split(",").collect::<Vec<_>>();
    match &exit_on_write[..] {
        [dest_addr_or_symbol] => {
            let dest_addr = parse_address_or_symbol(dest_addr_or_symbol, elf, elf_name)?;
            Ok((dest_addr as u64, None))
        }
        [dest_addr_or_symbol, write_value] => {
            let dest_addr = parse_address_or_symbol(dest_addr_or_symbol, elf, elf_name)?;
            let Some(write_addr) = parse_address(write_value) else {
                bail!("expected valid write address");
            };
            Ok((dest_addr as u64, Some(write_addr?)))
        }
        _ => bail!("invalid exit-on-write arguments"),
    }
}

fn parse_address(input: &str) -> Option<Result<u128>> {
    if input.starts_with("0x") {
        let input = input.trim_start_matches("0x");
        let address = u128::from_str_radix(input, 16);
        Some(address.context("invalid hex number"))
    } else if let Ok(address) = input.parse::<u128>() {
        Some(Ok(address))
    } else {
        None
    }
}

fn parse_address_or_symbol(input: &str, elf: &object::File<'_>, elf_name: &str) -> Result<u128> {
    if let Some(address) = parse_address(input) {
        address
    } else {
        let Some(label_address) = get_address_of_symbol(input, elf) else {
            bail!("could not find label {} in {}", input, elf_name,);
        };
        Ok(label_address as u128)
    }
}

fn get_address_of_symbol(name: &str, elf: &object::File<'_>) -> Option<u64> {
    let sym = elf.symbol_by_name(name);
    sym.map(|s| s.address())
}
