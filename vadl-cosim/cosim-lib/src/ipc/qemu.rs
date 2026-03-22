use std::{
    fs::File,
    path::PathBuf,
    process::{Child, Command, Stdio},
};

use color_eyre::{Result, eyre::Context};
use tracing::info;

use crate::{
    config::{self, Config},
    ipc::{cstructs::BrokerSHMRingBuffer, shm::SharedMemory},
};

type BrokerSHMRingBufferImpl = BrokerSHMRingBuffer<4>;

pub struct Client {
    pub id: String,
    pub shm: SharedMemory<BrokerSHMRingBufferImpl>,
    pub is_open: bool,
    pub process: Child,
    pub name: Option<String>,
    pub run_count: u64,
    pub stdout: Option<PathBuf>,
    pub stderr: Option<PathBuf>,
}

impl Client {
    pub fn create(config: &Config, client_idx: usize) -> Result<Self> {
        let client_cfg = config.for_client(client_idx);

        // ensure the client id is unique across parallel cosim-executions by appending the pid of
        // the cosimulator instance
        let pid = std::process::id();
        let client_id = format!("{pid}-{client_idx}");

        let mut shm: SharedMemory<BrokerSHMRingBufferImpl> =
            SharedMemory::create(&format!("/cosimulation-shm-{client_id}"))?;

        shm.get_mut().init()?;

        info!(
            client_id = client_id,
            "created shm and sems, spawning client"
        );

        let executable_path = client_cfg.exec.clone();
        let plugin_path = config.qemu.plugin.clone();
        let client_mode = match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => "insn",
            crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => "tb",
        };

        let mut plugin_args = vec![
            format!("client-id={client_id}"),
            format!("mode={client_mode}"),
            format!(
                "with-memory-checks={}",
                if config.testing.protocol.with_memory_checks {
                    1
                } else {
                    0
                }
            ),
        ];
        if let Some(client_name) = &client_cfg.name {
            plugin_args.push(format!("client-name={client_name}"));
        }

        let plugin = [vec![plugin_path], plugin_args].concat();
        let plugin = plugin.join(",");
        let default_args = vec![
            format!("-{}", client_cfg.pass_test_exec_to),
            client_cfg.test_exec.clone(),
            "-plugin".into(),
            plugin,
        ];

        let gdb_args = if client_cfg.gdb.enable {
            let remote_target = &client_cfg.gdb.remote_target;
            let gdb_args = match client_cfg.gdb.target_type {
                crate::config::GDBTargetType::Chardev => {
                    vec![
                        "-chardev".into(),
                        format!("socket,path={remote_target},server=on,wait=off,id=gdb{client_id}"),
                        "-gdb".into(),
                        format!("chardev:gdb{client_id}"),
                        "-S".into(),
                    ]
                }
                crate::config::GDBTargetType::Port => {
                    info!(
                        "Cosimulation with GDB-Debugging enabled, connect via: gdb -ex \"target remote {}\"",
                        remote_target
                    );
                    vec!["-gdb".into(), format!("{remote_target}"), "-S".into()]
                }
            };
            info!(
                "Cosimulation with GDB-Debugging enabled, connect via: gdb -ex \"target remote {}\"",
                remote_target
            );
            gdb_args
        } else {
            vec![]
        };

        let args = [default_args, client_cfg.additional_args.clone(), gdb_args].concat();

        info!(executable_path, ?args, "starting client");

        let mut client_process = Command::new(&executable_path);
        let client_process = client_process.args(args);
        let mut client_stdout = None;
        let mut client_stderr = None;

        if config.logging.enable {
            let stdout_path = config
                .logging
                .dir
                .join(format!("client-{client_id}-stdout"))
                .with_extension(config::COSIM_LOG_EXTENSION);

            let stderr_path = config
                .logging
                .dir
                .join(format!("client-{client_id}-stderr"))
                .with_extension(config::COSIM_LOG_EXTENSION);

            client_stdout = Some(stdout_path.clone());
            client_stderr = Some(stderr_path.clone());

            let stdout_file = File::create(stdout_path)?;
            let stderr_file = File::create(stderr_path)?;

            client_process
                .stdout(stdout_file.try_clone()?)
                .stderr(stderr_file.try_clone()?);
        } else {
            client_process.stdout(Stdio::null()).stderr(Stdio::null());
        }

        let client_process = client_process.spawn().wrap_err_with(|| {
            format!("Failed to create client with idx: {client_id} and path: {executable_path}")
        })?;

        Ok(Self {
            id: client_id,
            shm,
            is_open: true,
            process: client_process,
            name: client_cfg.name.clone(),
            run_count: 0,
            stdout: client_stdout,
            stderr: client_stderr,
        })
    }

    pub fn terminate(&mut self) -> Result<()> {
        self.process
            .kill()
            .with_context(|| format!("Failed to kill qemu-process: {}", self.id))
    }

    pub fn name_or_id(&self) -> String {
        self.name.clone().unwrap_or(self.id.to_string())
    }
}
