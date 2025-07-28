use clap::Parser;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
pub struct Cli {
    /// Path to the (toml) config file
    #[arg(short, long, value_name="FILE", default_value_t = default_config_file())]
    pub config: String,

    /// Defines where the test-executable is passed to when starting the QEMU-client
    #[arg(short, long, value_name = "FILE")]
    pub test_exec: Option<String>,

    #[arg(long)]
    pub tui: bool,
}

fn default_config_file() -> String {
    "./config.toml".into()
}
