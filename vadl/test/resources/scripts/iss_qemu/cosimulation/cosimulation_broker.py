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
It is responsible for starting QEMU-clients with the cosimulation plugin, comparing the state between them and produce a test-report.
The broker communicates with each QEMU-client using IPC.
"""

from ctypes import c_char, c_int, c_uint, c_uint64, c_uint8, sizeof, Structure, c_size_t, Union
from dataclasses import dataclass, asdict
import argparse
import os
import mmap
import subprocess
import multiprocessing
import posix_ipc as ipc
import atexit
import subprocess
from config import Config, Endian, load_config 
import json
from collections import deque

import logging
from typing import Annotated, Any, Optional

"""
The following classes represent the equally defined c-structs in the cosimulation QEMU plugin.
They are used to transfer data from a QEMU-client to the broker using shared memory.
See `BrokerSHM(Structure)` as the "entrypoint" of this class-hierarchy.
"""

class SHMString(Structure):
    MAX_LEN = 256
    _fields_ = [("len", c_size_t), ("value", c_char * MAX_LEN)]

    def __repr__(self):
        return f"SHMString(len={self.len}, value={self.fstr()})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self):
        return {"len": self.len, "value": self.fstr()}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.len: Annotated[int, c_size_t]
        self.value: Annotated[bytes, c_char * self.MAX_LEN]

    def fstr(self) -> str:
        return self.value[:self.len].decode()

class InsnData(Structure):
    MAX_INSN_DATA_SIZE = 256
    _fields_ = [("size", c_size_t), ("buffer", c_uint8 * MAX_INSN_DATA_SIZE)]

    def __repr__(self):
        # endianess here is just a guess since we cannot provide it here
        return f"InsnData(size={self.size}, buffer={self.fbuffer('little')})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian):
        return {"size": self.size, "buffer": self.fbuffer(endian)}

    def fbuffer(self, endian: Endian) -> str:
        order = reversed if endian == 'little' else lambda x: x
        res = b''.join(order([num.to_bytes() for num in self.buffer[:self.size]]))
        return res.hex(' ')

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.size: Annotated[int, c_size_t]
        self.buffer: Annotated[list[int], c_uint8 * self.MAX_INSN_DATA_SIZE]

class TBInsnInfo(Structure):
    _fields_ = [("pc", c_uint64), ("size", c_size_t), ("symbol", SHMString), ("hwaddr", SHMString), ("disas", SHMString), ("data", InsnData)]

    def __repr__(self):
        return f"TBInsnInfo(pc={self.pc}, symbol={self.symbol}, hwaddr={self.hwaddr}, disas={self.disas}, data={self.data})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian):
        return {"pc": self.pc, "size": self.size, "symbol": self.symbol.to_dict(), "hwaddr": self.hwaddr.to_dict(), "disas": self.disas.to_dict(), "data": self.data.to_dict(endian)}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.pc: Annotated[int, c_uint64]
        self.size: Annotated[int, c_size_t]
        self.symbol: Annotated[SHMString, SHMString]
        self.hwaddr: Annotated[SHMString, SHMString]
        self.disas: Annotated[SHMString, SHMString]
        self.data: Annotated[InsnData, InsnData]

class TBInfo(Structure):
    INSNS_INFOS_SIZE = 32
    _fields_ = [("pc", c_uint64), ("insns", c_size_t), ("insns_info_size", c_size_t), ("insns_info", TBInsnInfo * INSNS_INFOS_SIZE)]

    def __repr__(self):
        return f"TBInfo(pc={self.pc}, insns={self.insns}, insns_info_size={self.insns_info_size}, insns_info={self.insns_info[:self.insns_info_size]})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian):
        return {
            "pc": self.pc, 
            "insns": self.insns, 
            "insns_info_size": self.insns_info_size, 
            "insns_info": [insn.to_dict(endian) for insn in self.insns_info[:self.insns_info_size]]
        }

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.pc: Annotated[int, c_uint64]
        self.insns: Annotated[int, c_size_t]
        self.insns_info_size: Annotated[int, c_size_t]
        self.insns_info: Annotated[list[TBInsnInfo], TBInsnInfo * self.INSNS_INFOS_SIZE]


class BrokerSHM_TB(Structure):
    INFOS_SIZE = 1024
    _fields_ = [("size", c_size_t), ("infos", TBInfo * INFOS_SIZE)]

    def __repr__(self):
        return f"BrokerSHM_TB(size={self.size}, infos={self.infos[:self.size]})"
    
    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian):
        return {"size": self.size, "infos": [info.to_dict(endian) for info in self.infos[:self.size]]}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.size: Annotated[int, c_size_t]
        self.infos: Annotated[list[TBInfo], TBInfo * self.INFOS_SIZE]

class SHMRegister(Structure):
    MAX_REGISTER_DATA_SIZE = 64
    _fields_ = [("size", c_int), ("data", c_uint8 * MAX_REGISTER_DATA_SIZE), ("name", SHMString)]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.size: Annotated[int, c_int]
        self.data: Annotated[list[int], c_uint8 * self.MAX_REGISTER_DATA_SIZE]
        self.name: Annotated[SHMString, SHMString]

    def __repr__(self):
        # endianess here is just a guess since we cannot provide it here
        return f"SHMRegister(size={self.size}, data={self.fdata('little')}, name={self.name})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian, gdb_map: dict[str, str]):
        return {"size": self.size, "data": self.fdata(endian), "name": self.name.to_dict(), "name-mapped": self.fname(gdb_map)}

    def fname(self, gdb_map: dict[str, str]) -> str:
        n = self.name.fstr() # assume that the name is "printable"
        if n in gdb_map:
            return gdb_map[n]
        else:
            return n

    def fdata(self, endian: Endian) -> str:
        order = reversed if endian == 'little' else lambda x: x
        res = b''.join(order([num.to_bytes() for num in self.data[:self.size]]))
        return res.hex(' ')


class SHMCPU(Structure):
    MAX_CPU_REGISTERS = 256
    _fields_ = [("idx", c_uint), ("registers_size", c_size_t), ("registers", SHMRegister * MAX_CPU_REGISTERS)]

    def __repr__(self):
        # endianess here is just a guess since we cannot provide it here
        return f"SHMCPU(idx={self.idx}, registers_size={self.registers_size}, registers={self.registers[:self.registers_size]})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian, gdb_map: dict[str, str]):
        return {"idx": self.idx, "registers_size": self.registers_size, "registers": [reg.to_dict(endian, gdb_map) for reg in self.registers[:self.registers_size]]}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.idx: Annotated[int, c_uint]
        self.registers_size: Annotated[int, c_size_t]
        self.registers: Annotated[list[SHMRegister], SHMRegister * self.MAX_CPU_REGISTERS]

class BrokerSHM_Exec(Structure):
    MAX_CPU_COUNT = 8
    _fields_ = [("init_mask", c_int), ("cpus", SHMCPU * MAX_CPU_COUNT), ("insn_info", TBInsnInfo)]

    def __repr__(self):
        # endianess here is just a guess since we cannot provide it here
        return f"BrokerSHM_Exec(init_mask={self.init_mask}, cpus={self.cpus[:]}, insn_info={self.insn_info})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, endian: Endian, gdb_map: dict[str, str]):
        return {"init_mask": self.init_mask, "cpus": [cpu.to_dict(endian, gdb_map) for cpu in self.cpus[:]], "insn_info": self.insn_info.to_dict(endian)}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.init_mask: Annotated[int, c_int]
        self.cpus: Annotated[list[SHMCPU], SHMCPU * self.MAX_CPU_COUNT]
        self.insn_info: Annotated[TBInsnInfo, TBInsnInfo]

class BrokerSHM(Union):
    _fields_ = [("shm_tb", BrokerSHM_TB), ("shm_exec", BrokerSHM_Exec)]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.shm_tb: Annotated[BrokerSHM_TB, BrokerSHM_TB]
        self.shm_exec: Annotated[BrokerSHM_Exec, BrokerSHM_Exec]

@dataclass
class Client:
    id: int
    shm: ipc.SharedMemory
    shm_struct: BrokerSHM
    sem_server: ipc.Semaphore
    sem_client: ipc.Semaphore
    is_open: bool
    process: Optional[multiprocessing.Process]

clients: list[Client] = []

def run_with_callback(command, on_complete, config: Config, client: Client) -> multiprocessing.Process:
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

def run_lockstep(config: Config, traces: deque[list[dict[str, Any]]]) -> Report:
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
            - Do the amount of registers per CPU match between clients (potentially ignoring a configured set of registers)?
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

    gdb_reg_map_values = config.qemu.gdb_reg_map.values()
    def compare_client_state() -> list[ClientDiff]:
        diffs = []
        c1 = clients[0]
        c2 = clients[1]

        if config.testing.protocol.layer == 'insn':
            c1shm = c1.shm_struct.shm_exec
            c2shm = c2.shm_struct.shm_exec

            # Store the current state of each client as a trace
            d1 = c1shm.to_dict(config.qemu.endian, config.qemu.gdb_reg_map)
            d2 = c1shm.to_dict(config.qemu.endian, config.qemu.gdb_reg_map)
            traces.append([d1, d2])

            if c1shm.init_mask != c2shm.init_mask:
                diffs.append(ClientDiff("cpu.init_mask", f"{c1shm.init_mask:08b}", f"{c2shm.init_mask:08b}"))

            for cpu_index in range(BrokerSHM_Exec.MAX_CPU_COUNT):
                c1cpu = c1shm.cpus[cpu_index]
                c2cpu = c2shm.cpus[cpu_index]

                if not config.qemu.ignore_unset_registers and c1cpu.registers_size != c2cpu.registers_size:
                    diffs.append(ClientDiff(f"cpu.{cpu_index}.registers.size", f"{c1cpu.registers_size}", f"{c2cpu.registers_size}", "different number of CPU registers"))

                for reg_index in range(min(c1cpu.registers_size, c2cpu.registers_size)):
                    c1reg = c1cpu.registers[reg_index]
                    r1name = c1reg.fname(config.qemu.gdb_reg_map)

                    c2reg = c2cpu.registers[reg_index]
                    r2name = c2reg.fname(config.qemu.gdb_reg_map)

                    if r1name in config.qemu.ignore_registers or \
                        (config.qemu.ignore_unset_registers and not r1name in gdb_reg_map_values):
                        continue

                    if c1reg.size != c2reg.size:
                        diffs.append(ClientDiff(f"cpu.{cpu_index}.registers.{reg_index}.size", f"{c1reg.size}", f"{c2reg.size}", "reg sizes differ"))

                    if r1name != r2name:
                        diffs.append(ClientDiff(f"cpu.{cpu_index}.registers.{reg_index}.name", f"{r1name}", f"{r2name}", "reg names differ"))

                    r1data = c1reg.fdata(config.qemu.endian)
                    r2data = c2reg.fdata(config.qemu.endian)
                    if r1data != r2data:
                        diffs.append(ClientDiff(f"cpu.{cpu_index}.registers.{reg_index}.data", f"{r1data}", f"{r2data}", "reg data differ"))

            return diffs
        else:
            c1shm = c1.shm_struct.shm_tb
            c2shm = c2.shm_struct.shm_tb

            print("TODO: tb-block exec level testing")

            return []


    skip = config.testing.protocol.skip_n_instructions
    execute_remaining = config.testing.protocol.execute_all_remaining_instructions
    stop_after = config.testing.protocol.stop_after_n_instructions

    diffs = []

    # Lockstepping logic:
    # Release each client once and then compare their states
    # TODO: A client might need to be released multiple times for tb-level testing
    # NOTE: Maybe parallelize this for exec-level and tb-strict-level testing, 
    #       for tb-level testing this might not be possible due to the differently generated TBs
    while any(map(lambda c: c.is_open, clients)):
        for client in clients: 
            if client.is_open:
                try:
                    client.sem_client.release()

                    # wait at most 0.1 second, if an error occurs: assume that the client finished (crashing = finishing)
                    client.sem_server.acquire(0.1)
                except ipc.BusyError:
                    logger.debug(f"run_lockstep: BusyError: noticed that client #{client.id} shutdown. marking as closed")
                    client.is_open = False

        if skip > 0:
            skip -= 1 
            continue

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

def main(config: Config):
    logger.debug(f"starting broker: config={config}")

    # create shared memory and semaphores per client
    for i, client_cfg in enumerate(config.qemu.clients):
        shm = ipc.SharedMemory(f"/cosimulation-shm-{i}", ipc.O_CREX, size=sizeof(BrokerSHM))
        mm = mmap.mmap(shm.fd, sizeof(BrokerSHM))
        shm_struct = BrokerSHM.from_buffer(mm) 

        sem_server = ipc.Semaphore(f"/cosimulation-sem-server-{i}", ipc.O_CREX)
        sem_client = ipc.Semaphore(f"/cosimulation-sem-client-{i}", ipc.O_CREX)
        logger.info(f"created shm and sems, spawning client with id: {i}")

        executable_path = client_cfg.exec
        plugin_path = f"{config.qemu.plugin},client-id={i},mode={config.testing.protocol.layer}"
        default_args = ["-bios", config.testing.test_exec, "-plugin", plugin_path]
        args = default_args + client_cfg.additional_args
        logger.info(f"starting client: {" ".join([executable_path, *args])}")
        client = Client(i, shm, shm_struct, sem_server=sem_server, sem_client=sem_client, is_open=True, process=None)
        clients.append(client)
        client.process = run_with_callback([executable_path, *args], on_client_complete, config, client)

    if config.testing.protocol.mode == 'lockstep':
        max_trace_len = config.testing.max_trace_length
        traces = deque(maxlen=max_trace_len if max_trace_len >= 0 else None)
        report = run_lockstep(config, traces)
        j = {"report": asdict(report), "traces": list(traces)}
        result_file = os.path.join(config.testing.protocol.out.dir, "result.json")
        os.makedirs(os.path.dirname(result_file), exist_ok=True)
        with open(result_file, "w") as f:
            if config.testing.protocol.out.format == "json":
                f.write(json.dumps(j))
            else:
                logger.error(f"illegal testing output format: {config.testing.protocol.out.format}")
                exit(1)

        for client in clients:
            if client.process is not None:
                client.process.terminate()

    for client in clients:
        close_client(config, client)

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
        pass # ignore
    logger.debug(f"cleanup_sem of client #{client.id} done")

def cleanup_smh(config: Config, client: Client):
    try:
        client.shm.close_fd()
        client.shm.unlink()
    except ipc.ExistentialError:
        pass # ignore

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


def default_config_file() -> str:
    dir_path = os.path.dirname(os.path.realpath(__file__))
    return f"{dir_path}/config.toml"

if __name__ == '__main__':
    logger = logging.getLogger(__name__)
    parser = argparse.ArgumentParser(
        prog="Cosimulation Broker",
        description="Executes two (or more) qemu-instances in parallel which need to use the cosimulation plugin to connect to the broker."
    )
    parser.add_argument('-c', '--config', type=str, help="Path to the (toml) config file, default is: ./config.toml", default=default_config_file())
    args = parser.parse_args()

    config = load_config(args.config)
    if config is None:
        print("Couldn't load config. Stopping.")
        exit(1)
 
    if config.logging.enable:
        filemode = "w" if config.logging.clear_on_rerun else "a"
        filename = os.path.join(config.logging.dir, config.logging.file)
        os.makedirs(os.path.dirname(filename), exist_ok=True)
        logging.basicConfig(filename=filename, filemode=filemode, level=config.logging.level)
    else:
        logging.disable()

    if not config.dev.dry_run:
        atexit.register(cleanup, config)
        main(config)
    else:
        logger.info(f"Dry-Run. Config: {config}")

