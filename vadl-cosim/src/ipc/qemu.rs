use std::{
    fs::File, path::Path, process::{Child, Command}, sync::Arc, time::Duration
};

use anyhow::{Context, Result};
use tracing::{debug, error, info};

use crate::{
    config::Config,
    ipc::{
        cstructs::BrokerSHM,
        sem::{Semaphore, TimedWaitState},
        shm::SharedMemory,
    },
};

pub struct Client {
    pub id: usize,
    pub shm: Arc<SharedMemory<BrokerSHM>>,
    pub sem_server: Semaphore,
    pub sem_client: Semaphore,
    pub is_open: bool,
    pub process: Child,
    pub name: Option<String>,
    pub run_count: u64,
}

unsafe impl Send for Client {}

impl Client {
    pub fn run_n_times(&mut self, n: u32, config: &Config) {
        for _ in 0..n {
            if self.is_open {
                self.run(config);
            } else {
                break; // stop running if client already closed
            }
        }
    }

    pub fn run(&mut self, config: &Config) -> bool {
        assert!(self.is_open, "called client.run() on a closed client");

        self.run_count += 1;

        match self.run_inner(config) {
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
                            error!(
                                client_id = self.id,
                                "client is still running but unresponive"
                            );
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

    fn run_inner(&mut self, config: &Config) -> Result<bool> {
        self.sem_client.post()?;

        if config.for_client(self.id).gdb.enable {
            self.sem_server.wait()?;
            return Ok(true);
        }

        let wait_res = self.sem_server.timedwait(Duration::from_secs(1))?;
        match wait_res {
            TimedWaitState::Timeout => Ok(false),
            TimedWaitState::Success => Ok(true),
        }
    }

    pub fn create(config: &Config, client_idx: usize) -> Result<Self> {
        let client_cfg = config.for_client(client_idx);

        let shm: SharedMemory<BrokerSHM> =
            SharedMemory::create(&format!("/cosimulation-shm-{client_idx}"))?;
        let sem_server = Semaphore::create(&format!("/cosimulation-sem-server-{client_idx}"), 0)?;
        let sem_client = Semaphore::create(&format!("/cosimulation-sem-client-{client_idx}"), 0)?;

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

        let args = if client_cfg.gdb.enable {
            let gdb_options = vec!["-gdb".into(), format!("tcp::6000{client_idx}"), "-S".into()];
            [
                default_args,
                client_cfg.additional_args.clone(),
                gdb_options,
            ]
            .concat()
        } else {
            [default_args, client_cfg.additional_args.clone()].concat()
        };

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
            shm: shm.into(),
            sem_server,
            sem_client,
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
