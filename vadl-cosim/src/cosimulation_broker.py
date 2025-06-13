# SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

"""
The broker for cosimulation testing.
It is responsible for starting QEMU-clients with the cosimulation plugin,
comparing the state between them and produce a test-report.
The broker communicates with each QEMU-client using IPC.
"""

import atexit
import json
import logging
import mmap
import multiprocessing
import os
import subprocess
from collections import deque
from ctypes import sizeof
from dataclasses import asdict, dataclass, field
from typing import Any, Optional, TypeAlias

import posix_ipc as ipc  # type: ignore[import-not-found]

from src.config import Config
from src.cstructs import SHMCPU, BrokerSHM, BrokerSHM_Exec, SHMRegister

logger = logging.getLogger(__name__)


@dataclass
class Client:
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


clients: list[Client] = []


def run_with_callback(
    command, on_complete, config: Config, client: Client
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


def on_client_complete(returncode: int, config: Config, client: Client):
    logger.info(f"Process (client: {client.id}) finished with code: {returncode}")
    client.is_open = False


@dataclass
class ClientDiff:
    """
    Contains information about a divergence that was found during testing.
    """

    key: str
    """Represents the location of the diverged data in the BrokerSHM struct"""

    expected: str
    actual: str
    description: Optional[str] = None
    ref_expected: dict[str, str] = field(default_factory=dict[str, str])
    ref_actual: dict[str, str] = field(default_factory=dict[str, str])


@dataclass
class Report:
    """
    A report of the test-result. Returned / Written to disk after at the end of the test-run.
    If passed = True, then diffs will be empty since no divergence was found.
    """

    passed: bool
    diffs: list[ClientDiff]


def report_from_diffs(diffs: list[ClientDiff]) -> Report:
    if len(diffs) == 0:
        return Report(passed=True, diffs=diffs)
    else:
        return Report(passed=False, diffs=diffs)


Trace: TypeAlias = list[dict[str, Any]]


def diff_cpus(
    cpus1: list[SHMCPU],
    init_mask1: int,
    cpus2: list[SHMCPU],
    init_mask2: int,
    config: Config,
) -> list[ClientDiff]:
    diffs: list[ClientDiff] = []

    if init_mask1 != init_mask2:
        diffs.append(
            ClientDiff(
                "cpu.init_mask",
                f"{init_mask1:08b}",
                f"{init_mask1:08b}",
            )
        )
        return diffs

    for idx in range(BrokerSHM_Exec.MAX_CPU_COUNT):
        if init_mask1 & (1 << idx):
            cpu1 = cpus1[idx]
            cpu2 = cpus2[idx]
            diffs.extend(diff_cpu(cpu1, cpu2, idx, config))

    return diffs


# assumes that the name exists
def reg_by_name(registers: list[SHMRegister], name: str, config: Config) -> SHMRegister:
    for reg in registers:
        reg_name = reg.fname(config.qemu.gdb_reg_map)
        if reg_name == name:
            return reg

        if (
            name in config.qemu.gdb_reg_map
            and reg_name == config.qemu.gdb_reg_map[name]
        ):
            return reg

    assert False, "reg_by_name called but no register with that name found"


def diff_cpu(
    cpu1: SHMCPU, cpu2: SHMCPU, cpu_index: int, config: Config
) -> list[ClientDiff]:
    diffs: list[ClientDiff] = []

    if (
        not config.qemu.ignore_unset_registers
        and cpu1.registers_size != cpu2.registers_size
    ):
        diffs.append(
            ClientDiff(
                f"cpu[{cpu_index}].registers.size",
                f"{cpu1.registers_size}",
                f"{cpu2.registers_size}",
                "different number of CPU registers",
            )
        )

    if cpu1.registers_size < cpu2.registers_size:
        for reg_index in range(cpu1.registers_size):
            c1reg = cpu1.registers[reg_index]
            c2reg = reg_by_name(cpu2.registers, c1reg.name.fstr(), config)

            diffs.extend(diff_register(c1reg, c2reg, cpu_index, reg_index, config))
    else:
        for reg_index in range(cpu2.registers_size):
            c2reg = cpu2.registers[reg_index]
            c1reg = reg_by_name(cpu1.registers, c2reg.name.fstr(), config)

            diffs.extend(diff_register(c1reg, c2reg, cpu_index, reg_index, config))

    return diffs


def diff_register(
    reg1: SHMRegister, reg2: SHMRegister, cpu_index: int, reg_index: int, config: Config
) -> list[ClientDiff]:
    diffs: list[ClientDiff] = []

    r1name = reg1.fname(config.qemu.gdb_reg_map)
    r2name = reg2.fname(config.qemu.gdb_reg_map)

    if r1name in config.qemu.ignore_registers or (
        config.qemu.ignore_unset_registers
        and r1name not in config.qemu.gdb_reg_map.values()
    ):
        return diffs

    if reg1.size != reg2.size:
        diffs.append(
            ClientDiff(
                f"cpu[{cpu_index}].registers[{reg_index}].size",
                f"{reg1.size}",
                f"{reg2.size}",
                "reg sizes differ",
            )
        )

    if r1name != r2name:
        diffs.append(
            ClientDiff(
                f"cpu[{cpu_index}].registers[{reg_index}].name",
                f"{r1name}",
                f"{r2name}",
                "reg names differ",
            )
        )

    r1data = reg1.fdata()
    r2data = reg2.fdata()
    if r1data != r2data:
        diffs.append(
            ClientDiff(
                f"cpu[{cpu_index}].registers[{reg_index}].data",
                f"{r1data}",
                f"{r2data}",
                "reg data differ",
                ref_expected=reg1.to_dict(config.qemu.gdb_reg_map),
                ref_actual=reg2.to_dict(config.qemu.gdb_reg_map),
            )
        )

    return diffs


def run_lockstep(config: Config, traces: deque[Trace]) -> Report:
    """
    Runs the configured QEMU-clients in lockstep - meaning they are synchronized after each *execution-step*.
    The definition of an execution-step depends on the configured layer (`config.testing.protocol.layer`).

    Execution-Step based on layer:
        - "insn": execution-step = execution of a single instruction
        - "tb": execution-step = execution of one or multiple translation-blocks
                multiple TBs might be executed in a single execution-step if another client generated a *larger* TB
                this means that clients can still synchronize if the instructions are the same and only the TBs differ
        - "tb-strict": execution-step = execution of one translation-block
                       similar to "tb" but now the generated TBs between all clients must be identical

    Currently the following values are tested:
        - CPU:
            - Which CPUs are used? / How many CPUs are used?
            - Do the amount of registers per CPU match between clients
                (potentially ignoring a configured set of registers)?
        - Registers:
            - Do register-sizes match between clients?
            - Do register-names match between clients?
            - Do register-data match between clients?

    The function will return after the first execution-step that introduced a divergence.
    If multiple diffs are found in after a single execution-step then all of them will be reported.

    Parameters:
        config (Config)
        traces: deque[list[dict[str, Any]]]: Collects the state of each client after each execution-step.
    """

    def compare_client_state() -> list[ClientDiff]:
        diffs = []
        trace_entry = []
        for c in clients:
            if config.testing.protocol.layer == "insn":
                d = c.shm_struct.shm_exec.to_dict(config.qemu.gdb_reg_map)
                trace_entry.append(d)
            else:
                d = c.shm_struct.shm_tb.to_dict(config.qemu.gdb_reg_map)
                trace_entry.append(d)
        traces.append(trace_entry)

        for i in range(len(clients)):
            for j in range(i + 1, len(clients)):
                c1 = clients[i]
                c2 = clients[j]

                if config.testing.protocol.layer == "insn":
                    c1insn = c1.shm_struct.shm_exec
                    c2insn = c2.shm_struct.shm_exec

                    diffs.extend(
                        diff_cpus(
                            c1insn.cpus,
                            c1insn.init_mask,
                            c2insn.cpus,
                            c2insn.init_mask,
                            config,
                        )
                    )

                    return diffs
                else:
                    c1tb = c1.shm_struct.shm_tb
                    c2tb = c2.shm_struct.shm_tb

                    diffs.extend(
                        diff_cpus(
                            c1tb.cpus,
                            c1tb.init_mask,
                            c2tb.cpus,
                            c2tb.init_mask,
                            config,
                        )
                    )
                    # NOTE: also diff instruction info especially for "tb-strict"

                    return diffs

        return []

    skip_per_client = [client.skip_n_instructions for client in config.qemu.clients]

    # Skip first n instructions per client
    while any(map(lambda c: c.is_open, clients)) and any(
        map(lambda skip: skip > 0, skip_per_client)
    ):
        for i, client in enumerate(clients):
            if client.is_open and skip_per_client[i] > 0:
                skip_per_client[i] -= 1
                client.run()

    execute_remaining = config.testing.protocol.execute_all_remaining_instructions
    stop_after = config.testing.protocol.stop_after_n_instructions

    diffs: list[ClientDiff] = []

    # Lockstepping logic:
    # Release each client once and then compare their states
    # NOTE: Maybe parallelize this for exec-level and tb-strict-level testing,
    #       for tb-level testing this might not be possible due to the differently generated TBs

    if (
        config.testing.protocol.layer == "insn"
        or config.testing.protocol.layer == "tb-strict"
    ):
        while any(map(lambda c: c.is_open, clients)):
            for client in clients:
                if client.is_open:
                    client.run()

            if not execute_remaining:
                if stop_after > 0:
                    stop_after -= 1
                else:
                    return report_from_diffs(diffs)

            diffs += compare_client_state()

            # early exit for lockstepping
            if len(diffs) > 0:
                return report_from_diffs(diffs)
    else:

        @dataclass
        class ClientSyncInfo:
            start_pc: int
            end_pc: int
            tb_size: int
            client_idx: int

            def is_jump(self) -> bool:
                return self.start_pc + self.tb_size * 4 != self.end_pc

            def __str__(self):
                return (
                    f"ClientSyncInfo(start_pc={hex(self.start_pc)}, end_pc={hex(self.end_pc)}, "
                    f"tb_size={self.tb_size}, client_idx={self.client_idx})"
                )

            def __repr__(self):
                return str(self)

        # TB-Syncing:
        # Why?: Necessary to ensure that on a TB-level cosimulation, the state of all clients is compared at the same PC
        # Syncing is only done for the "tb" layer,
        #   the "tb-strict" layer assumes that all TBs across all clients are equal.
        def sync_clients(clients_sync_infos: list[ClientSyncInfo]):
            logger.debug(f"starting to sync clients: {clients_sync_infos}")

            if len(clients_sync_infos) == 0:
                return

            for i in range(len(clients_sync_infos) - 1):
                assert (
                    clients_sync_infos[i].start_pc == clients_sync_infos[i + 1].start_pc
                ), f"Clients should initially be synced: {clients_sync_infos}"

            jumped_client: Optional[ClientSyncInfo] = None
            for client_sync_info in clients_sync_infos:
                if client_sync_info.is_jump():
                    logger.debug(
                        f"noticed that client jumped: {client_sync_info}"
                    )
                    jumped_client = client_sync_info
                    break

            target_pc = (
                jumped_client.end_pc
                if jumped_client is not None
                else max(map(lambda tbs: tbs.end_pc, clients_sync_infos))
            )
            clients_queue = deque(
                filter(lambda tbs: tbs.end_pc != target_pc, clients_sync_infos)
            )

            while len(clients_queue) > 0:
                logger.debug(
                    f"queue: {clients_queue}, tbs: {clients_sync_infos}, max: {target_pc}"
                )
                client_entry = clients_queue.popleft()
                client = clients[client_entry.client_idx]

                start_pc = client.shm_struct.shm_tb.tb_info.pc
                tb_size = client.shm_struct.shm_tb.tb_info.insns_info_size

                # NOTE: closed clients won't be readded to the queue
                if client.run():
                    end_pc = client.shm_struct.shm_tb.tb_info.pc
                    sync_info = ClientSyncInfo(
                        start_pc=start_pc,
                        end_pc=end_pc,
                        tb_size=tb_size,
                        client_idx=client_entry.client_idx,
                    )

                    if jumped_client is None and end_pc > target_pc:
                        raise ValueError("Client diverged irrecoverably, fail test.")

                    if end_pc != target_pc:
                        clients_queue.append(sync_info)

        while any(map(lambda c: c.is_open, clients)):
            clients_tbs: list[ClientSyncInfo] = []
            for i, client in enumerate(clients):
                if client.is_open:
                    start_pc = client.shm_struct.shm_tb.tb_info.pc
                    tb_size = client.shm_struct.shm_tb.tb_info.insns_info_size
                    if client.run():
                        end_pc = client.shm_struct.shm_tb.tb_info.pc
                        clients_tbs.append(
                            ClientSyncInfo(
                                start_pc=start_pc,
                                end_pc=end_pc,
                                tb_size=tb_size,
                                client_idx=i,
                            )
                        )

            try:
                sync_clients(clients_tbs)
            except ValueError as err:
                # Some client diverged
                logger.exception(err, exc_info=True)
                return report_from_diffs(diffs)

            if not execute_remaining:
                if stop_after > 0:
                    stop_after -= 1
                else:
                    return report_from_diffs(diffs)

            diffs += compare_client_state()

            # early exit for lockstepping
            if len(diffs) > 0:
                return report_from_diffs(diffs)

    # if the loop has exited and no diffs were found then the test passed
    return report_from_diffs(diffs)


"""
Cleanup functions for the created IPCs
TODO: Maybe put IPC related logic in a separate file
"""


def cleanup_sem(config: Config, client: Client):
    logger.debug(f"cleanup_sem of client #{client.id} start")
    try:
        client.sem_client.unlink()
        client.sem_server.unlink()
        client.sem_client.close()
        client.sem_server.close()
    except ipc.ExistentialError:
        pass  # ignore
    logger.debug(f"cleanup_sem of client #{client.id} done")


def cleanup_smh(config: Config, client: Client):
    try:
        client.shm.close_fd()
        client.shm.unlink()
    except ipc.ExistentialError:
        pass  # ignore


def close_client(config: Config, client: Client):
    logger.info(f"Closing client: {client.id}")
    if client.process is not None:
        client.process.terminate()


def cleanup_client(config: Config, client: Client):
    cleanup_sem(config, client)
    cleanup_smh(config, client)
    close_client(config, client)


def cleanup(config: Config):
    logger.info("cleaning up shm and sems for each client")
    for client in clients:
        cleanup_client(config, client)


def start(config: Config):
    logger.debug(f"starting broker: config={config}")
    atexit.register(cleanup, config)

    # create shared memory and semaphores per client
    for i, client_cfg in enumerate(config.qemu.clients):
        shm = ipc.SharedMemory(
            f"/cosimulation-shm-{i}", ipc.O_CREX, size=sizeof(BrokerSHM)
        )
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
        client = Client(
            i,
            shm,
            shm_struct,
            sem_server=sem_server,
            sem_client=sem_client,
            is_open=True,
            process=None,
            name=client_cfg.name,
        )
        clients.append(client)
        client.process = run_with_callback(
            [executable_path, *args], on_client_complete, config, client
        )

    if config.testing.protocol.mode == "lockstep":
        max_trace_len = config.testing.max_trace_length
        traces: deque[Trace] = deque(
            maxlen=max_trace_len if max_trace_len >= 0 else None
        )
        report = run_lockstep(config, traces)
        named_traces = {
            "names": [
                client.name if client.name is not None else str(client.id)
                for client in clients
            ],
            "traces": list(traces),
        }

        j = {"report": asdict(report), "traces": named_traces}

        result_file = os.path.join(config.testing.protocol.out.dir, "result.json")
        os.makedirs(os.path.dirname(result_file), exist_ok=True)
        with open(result_file, "w") as f:
            if config.testing.protocol.out.format == "json":
                f.write(json.dumps(j))
            else:
                logger.error(
                    f"illegal testing output format: {config.testing.protocol.out.format}"
                )
                exit(1)

        for client in clients:
            if client.process is not None:
                client.process.terminate()

    for client in clients:
        close_client(config, client)
