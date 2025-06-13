import logging
import mmap
import multiprocessing
import os
import subprocess
from ctypes import sizeof
from dataclasses import dataclass
from typing import Optional

import posix_ipc as ipc  # type: ignore[import-not-found]

from src.config import Client, Config
from src.cstructs import BrokerSHM

logger = logging.getLogger(__name__)


@dataclass
class QEMUClient:
    id: int
    shm: ipc.SharedMemory
    shm_struct: BrokerSHM
    sem_server: ipc.Semaphore
    sem_client: ipc.Semaphore
    is_open: bool
    process: Optional[multiprocessing.Process]
    name: Optional[str]

    def run(self) -> bool:
        """
        Releases the client to run for one execution-step.
        Returns True if the client successfully ran,
        False indicates that the client crashed or finished executing
        in both cases the client is marked as closed
        by setting self.is_open = False
        """
        try:
            self.sem_client.release()

            # wait at most 0.1 second, if an error occurs: assume that the client finished (crashing = finishing)
            self.sem_server.acquire(0.1)
            return True
        except ipc.BusyError:
            logger.debug(
                f"BusyError: noticed that client #{self.id} shutdown. marking as closed"
            )
            self.is_open = False
            return False


"""
Setup and Cleanup functions for the QEMU/C IPC
"""


def create_client(config: Config, client_cfg: Client, i: int) -> QEMUClient:
    shm = ipc.SharedMemory(f"/cosimulation-shm-{i}", ipc.O_CREX, size=sizeof(BrokerSHM))
    mm = mmap.mmap(shm.fd, sizeof(BrokerSHM))
    shm_struct = BrokerSHM.from_buffer(mm)

    sem_server = ipc.Semaphore(f"/cosimulation-sem-server-{i}", ipc.O_CREX)
    sem_client = ipc.Semaphore(f"/cosimulation-sem-client-{i}", ipc.O_CREX)
    logger.info(f"created shm and sems, spawning client with id: {i}")

    executable_path = client_cfg.exec

    plugin_path = config.qemu.plugin
    client_mode = ""
    if (
        config.testing.protocol.layer == "tb"
        or config.testing.protocol.layer == "tb-strict"
    ):
        client_mode = "tb"
    else:
        client_mode = config.testing.protocol.layer

    plugin_args = [f"client-id={i}", f"mode={client_mode}"]
    if client_cfg.name is not None:
        plugin_args += [f"client-name={client_cfg.name}"]

    plugin = ",".join([plugin_path] + plugin_args)

    default_args = [
        f"-{client_cfg.pass_test_exec_to}",
        config.testing.test_exec,
        "-plugin",
        plugin,
    ]
    args = default_args + client_cfg.additional_args
    logger.info(f"starting client: {' '.join([executable_path, *args])}")
    client = QEMUClient(
        i,
        shm,
        shm_struct,
        sem_server=sem_server,
        sem_client=sem_client,
        is_open=True,
        process=None,
        name=client_cfg.name,
    )
    client.process = run_with_callback(
        [executable_path, *args], on_client_complete, config, client
    )

    return client


def run_with_callback(
    command, on_complete, config: Config, client: QEMUClient
) -> multiprocessing.Process:
    """
    Starts a QEMU-client using pythons multiprocessing.Process
    After completion, the client is marked as done using client.is_open = False
    Both stdout and stderr for each client is redirected to a dedicated file.
    """

    def runner():
        stdout_path = os.path.join(config.logging.dir, f"client-{client.id}-stdout.txt")
        stderr_path = os.path.join(config.logging.dir, f"client-{client.id}-stderr.txt")
        stdout_file = open(stdout_path, "w")
        stderr_file = open(stderr_path, "w")
        process = subprocess.Popen(command, stdout=stdout_file, stderr=stderr_file)
        process.wait()
        stdout_file.close()
        stderr_file.close()
        on_complete(process.returncode, config, client)

    p = multiprocessing.Process(target=runner)
    p.start()
    return p


def on_client_complete(returncode: int, config: Config, client: QEMUClient):
    logger.info(f"Process (client: {client.id}) finished with code: {returncode}")
    client.is_open = False


def cleanup_sem(config: Config, client: QEMUClient):
    logger.debug(f"cleanup_sem of client #{client.id} start")
    try:
        client.sem_client.unlink()
        client.sem_server.unlink()
        client.sem_client.close()
        client.sem_server.close()
    except ipc.ExistentialError:
        pass  # ignore
    logger.debug(f"cleanup_sem of client #{client.id} done")


def cleanup_smh(config: Config, client: QEMUClient):
    try:
        client.shm.close_fd()
        client.shm.unlink()
    except ipc.ExistentialError:
        pass  # ignore


def close_client(config: Config, client: QEMUClient):
    logger.info(f"Closing client: {client.id}")
    if client.process is not None:
        client.process.terminate()


def cleanup_client(config: Config, client: QEMUClient):
    cleanup_sem(config, client)
    cleanup_smh(config, client)
    close_client(config, client)


def cleanup(config: Config, clients: list[QEMUClient]):
    logger.info("cleaning up shm and sems for each client")
    for client in clients:
        cleanup_client(config, client)
