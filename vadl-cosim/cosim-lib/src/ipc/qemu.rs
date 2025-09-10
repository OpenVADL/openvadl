use std::{
    fs::File,
    path::Path,
    process::{Child, Command},
    time::Duration,
};

use anyhow::{Context, Result};
use tracing::{debug, error, info};

use crate::{
    config::Config,
    ipc::{
        cstructs::{BrokerSHMRingBuffer, BrokerSem},
        sem::{Semaphore, TimedWaitState},
        shm::SharedMemory,
    },
};

type BrokerSHMRingBuffer32 = BrokerSHMRingBuffer<4>;

pub struct Client {
    pub id: usize,
    // TODO: refactor constant size
    pub shm: SharedMemory<BrokerSHMRingBuffer32>,
    pub sem: SharedMemory<BrokerSem>,
    pub is_open: bool,
    pub process: Child,
    pub name: Option<String>,
    pub run_count: u64,
}

impl Client {
    // TODO: maybe check whether all skips were successful for easier initial setup debugging
    pub fn skip_n_times(&mut self, n: u32, config: &Config) {
        for _ in 0..n {
            if self.is_open {
                let _ = self.skip(config);
            } else {
                break; // stop running if client already closed
            }
        }
    }

    /// skip does not increment the run_count, which is important for tracing information
    #[must_use]
    pub fn skip(&mut self, config: &Config) -> bool {
        assert!(self.is_open, "called client.skip() on a closed client");

        let run_result = self.run_inner(config);
        self.handle_run_result(run_result)
    }

    #[must_use]
    pub fn run(&mut self, config: &Config) -> bool {
        assert!(self.is_open, "called client.run() on a closed client");

        self.run_count += 1;

        let run_result = self.run_inner(config);
        self.handle_run_result(run_result)
    }

    #[must_use]
    fn handle_run_result(&mut self, run_result: Result<bool>) -> bool {
        match run_result {
            Ok(wait_res) => {
                if !wait_res {
                    debug!(
                        self.id,
                        self.name, self.is_open, "client did not respond - marking as closed"
                    );
                    self.is_open = false;
                    let process_status = self.process.try_wait();
                    match process_status {
                        Ok(Some(process_status)) => {
                            info!(
                                exit_code = process_status.code(),
                                client_id = self.id,
                                "client finished sucessfully"
                            )
                        }
                        Ok(None) => {
                            // error!(
                            //     client_id = self.id,
                            //     is_server = self.sem.get_sync().is_server,
                            //     "client is still running but unresponive"
                            // );
                        }
                        Err(err) => error!(
                            client_id = self.id,
                            ?err,
                            "failed to call try_wait on client"
                        ),
                    }
                }

                wait_res
            }
            Err(e) => {
                panic!("failed to run client: {e}")
            }
        }
    }

    #[must_use]
    fn run_inner(&mut self, config: &Config) -> Result<bool> {
        self.sem.release_client()?;

        if config.for_client(self.id).gdb.enable {
            self.sem.wait_client()?;
            return Ok(true);
        }

        let wait_res = self.sem.timedwait_client(Duration::from_secs(1))?;
        match wait_res {
            TimedWaitState::Timeout => Ok(false),
            TimedWaitState::Success => Ok(true),
        }
    }

    pub fn create(config: &Config, client_idx: usize) -> Result<Self> {
        let client_cfg = config.for_client(client_idx);

        let mut shm: SharedMemory<BrokerSHMRingBuffer32> =
            SharedMemory::create(&format!("/cosimulation-shm-{client_idx}"))?;

        *shm.get_mut() = BrokerSHMRingBuffer32::new()?;

        let mut sem: SharedMemory<BrokerSem> =
            SharedMemory::create(&format!("/cosimulation-sem-{client_idx}"))?;
        sem.get_mut().sync = Semaphore::create()?;

        info!(
            client_id = client_idx,
            "created shm and sems, spawning client"
        );

        let executable_path = client_cfg.exec.clone();
        let plugin_path = config.qemu.plugin.clone();
        let client_mode = match config.testing.protocol.layer {
            crate::config::ProtocolLayer::Insn => "insn",
            crate::config::ProtocolLayer::TB | crate::config::ProtocolLayer::TBStrict => "tb",
        };

        let mut plugin_args = vec![
            format!("client-id={client_idx}"),
            format!("mode={client_mode}"),
        ];
        if let Some(client_name) = &client_cfg.name {
            plugin_args.push(format!("client-name={client_name}"));
        }

        let plugin = [vec![plugin_path], plugin_args].concat();
        let plugin = plugin.join(",");
        let default_args = vec![
            format!("-{}", client_cfg.pass_test_exec_to),
            config.testing.test_exec.clone(),
            "-plugin".into(),
            plugin,
        ];

        let gdb_args = if client_cfg.gdb.enable {
            let remote_target = &client_cfg.gdb.remote_target;
            let gdb_args = match client_cfg.gdb.target_type {
                crate::config::GDBTargetType::Chardev => {
                    vec![
                        "-chardev".into(),
                        format!(
                            "socket,path={remote_target},server=on,wait=off,id=gdb{client_idx}"
                        ),
                        "-gdb".into(),
                        format!("chardev:gdb{client_idx}"),
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

        let base_path = Path::new(&config.logging.dir);
        let stdout_path = base_path.join(format!("client-{client_idx}-stdout.txt"));
        let stderr_path = base_path.join(format!("client-{client_idx}-stderr.txt"));

        let stdout_file = File::create(stdout_path)?;
        let stderr_file = File::create(stderr_path)?;

        let client_process = Command::new(executable_path)
            .args(args)
            .stdout(stdout_file)
            .stderr(stderr_file)
            .spawn()
            .with_context(|| format!("Failed to create client with idx: {client_idx}"))?;

        Ok(Self {
            id: client_idx,
            shm,
            sem,
            is_open: true,
            process: client_process,
            name: client_cfg.name.clone(),
            run_count: 0,
        })
    }

    pub fn terminate(&mut self) -> Result<()> {
        self.process
            .kill()
            .with_context(|| format!("failed to kill qemu-process: {}", self.id))
    }
}
