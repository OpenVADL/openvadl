from ctypes import c_char, c_int, c_uint, c_uint64, c_uint8, sizeof, Structure, c_size_t, Union
from dataclasses import dataclass
import argparse
import os
import mmap
import subprocess
import multiprocessing
from time import sleep, time
import posix_ipc as ipc
import atexit
import threading
import subprocess
from config import Config, load_config 

import logging
from typing import Annotated, Any, Literal, Optional

reference_ricsv64_exe = "/qemu-system-riscv64"
our_riscv64_exe = "/qemu-system-rv64im"

class SHMString(Structure):
    MAX_LEN = 256
    _fields_ = [("len", c_size_t), ("value", c_char * MAX_LEN)]

    def __repr__(self):
        values = ", ".join(f"{name}={value}" 
                          for name, value in self._asdict().items())
        return f"<{self.__class__.__name__}: {values}>"
    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.len: Annotated[int, c_size_t]
        self.value: Annotated[bytes, c_char * self.MAX_LEN]

    def fstr(self) -> bytes:
        return self.value[:self.len]

class TBInsnInfo(Structure):
    _fields_ = [("pc", c_uint64), ("size", c_size_t), ("symbol", SHMString), ("hwaddr", SHMString), ("disas", SHMString)]

    def __repr__(self):
        return f"<{self.__class__.__name__}: pc={self.pc}, size={self.size}, symbol={self.symbol}, hwaddr={self.hwaddr}, disas={self.disas}>"

    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.pc: Annotated[int, c_uint64]
        self.size: Annotated[int, c_size_t]
        self.symbol: Annotated[SHMString, SHMString]
        self.hwaddr: Annotated[SHMString, SHMString]
        self.disas: Annotated[SHMString, SHMString]

class TBInfo(Structure):
    INSNS_INFOS_SIZE = 32
    _fields_ = [("pc", c_uint64), ("insns", c_size_t), ("insns_info_size", c_size_t), ("insns_info", TBInsnInfo * INSNS_INFOS_SIZE)]

    def __repr__(self):
        values = f"pc={self.pc}, insns={self.insns}, insns_info_size={self.insns_info_size}, insns_info={self.insns_info[:self.insns_info_size]}"
        return f"<{self.__class__.__name__}: {values}>"
    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}

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
        values = f"size={self.size}, infos={self.infos[:self.size]}"
        return f"<{self.__class__.__name__}: {values}>"
    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}

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

    def fname(self, gdb_map: dict[str, str]) -> str:
        n = self.name.fstr().decode() # assume that the name is "printable"
        if n in gdb_map:
            return gdb_map[n]
        else:
            return n

    def fdata(self, endian: Literal['big', 'little']) -> str:
        # val = ''.join('{:02x}'.format(self.data[idx]) for idx in range(self.size))
        # if endian == 'big':
        #     val = val[::-1]
        # return f'0x{val}'
        order = reversed if endian == 'little' else lambda x: x
        res = b''.join(order([num.to_bytes() for num in self.data[:self.size]]))
        return res.hex(' ')


class SHMCPU(Structure):
    MAX_CPU_REGISTERS = 256
    _fields_ = [("idx", c_uint), ("registers_size", c_size_t), ("registers", SHMRegister * MAX_CPU_REGISTERS)]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.idx: Annotated[int, c_uint]
        self.registers_size: Annotated[int, c_size_t]
        self.registers: Annotated[list[SHMRegister], SHMRegister * self.MAX_CPU_REGISTERS]

class BrokerSHM_Exec(Structure):
    MAX_CPU_COUNT = 8
    _fields_ = [("init_mask", c_int), ("cpus", SHMCPU * MAX_CPU_COUNT), ("current_tb", TBInfo)]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.init_mask: Annotated[int, c_int]
        self.cpus: Annotated[list[SHMCPU], SHMCPU * self.MAX_CPU_COUNT]
        self.current_tb: Annotated[TBInfo, TBInfo]

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
    def runner():
        process = subprocess.Popen(command)
        process.wait()
        on_complete(process.returncode, config, client)
    
    p = multiprocessing.Process(target=runner)
    p.start()
    return p

# Usage
def callback(returncode: int, config: Config, client: Client):
    logger.info(f"Process (client: {client.id}) finished with code: {returncode}")
    client.is_open = False

def format_register_value(reg: SHMRegister) -> str:
    logger.debug(f"format_register_value::{len(reg.data)}, {reg.size}")
    val = ''.join('{:02x}'.format(reg.data[idx]) for idx in range(reg.size))
    return f'0x{val}'


# reading is possible if a client is done
def read(client: Client, config: Config) -> Optional[TBInfo]:
    if config.testing.protocol.layer == 'exec':
        shm = client.shm_struct.shm_exec
        logger.debug(f"reading client #{client.id}: reading exec")
        for cpu_index in range(BrokerSHM_Exec.MAX_CPU_COUNT):
            logger.debug(f"init_mask: {shm.init_mask}")
            if not shm.init_mask & (1 << cpu_index):
                logger.debug(f"Cpu at index {cpu_index} is not initialized")
                continue

            cpu = shm.cpus[cpu_index]
            logger.info(f"Reading client #{client.id}: CPU={cpu.idx}:")
            for reg in cpu.registers[:cpu.registers_size]:
                logger.info(f"\tReg: {reg.name} = {format_register_value(reg)}")

    elif config.testing.protocol.layer == 'tb':
        logger.debug(f"reading client #{client.id}: reading tb")
        shm = client.shm_struct.shm_tb
        for info in shm.infos:
            for insn in info.insns_info:
                if insn.pc > 0:
                    logger.info(f"Reading client #{client.id}: {insn.disas.value.decode()}")
    else:
        raise ValueError("illegal protocol layer: ", config.testing.protocol.layer)

    return None

def collect_disas(client: Client, config: Config) -> str:
    if config.testing.protocol.layer == 'tb':
        disas = []
        shm = client.shm_struct.shm_tb
        for info in shm.infos:
            disas.append(format_tbinfo(info))
        return "\n".join(disas)
    else:
        return "done!"

def format_tbinfo(info: TBInfo) -> str:
    disas = []
    for insn in info.insns_info:
        if insn.pc > 0:
            disas.append(insn.disas.value.decode())
    return "\n".join(disas)

def run_lockstep(config: Config):
    gdb_reg_map_values = config.qemu.gdb_reg_map.values()
    def compare_client_state() -> Optional[str]:
        c1 = clients[0]
        c2 = clients[1]

        if config.testing.protocol.layer == 'exec':
            c1shm = c1.shm_struct.shm_exec
            c2shm = c2.shm_struct.shm_exec

            print(f"1: {format_tbinfo(c1shm.current_tb)}\n2:{format_tbinfo(c2shm.current_tb)}")

            if c1shm.init_mask != c2shm.init_mask:
                return "init masks differ"

            for cpu_index in range(BrokerSHM_Exec.MAX_CPU_COUNT):
                c1cpu = c1shm.cpus[cpu_index]
                c2cpu = c2shm.cpus[cpu_index]

                if c1cpu.idx != c2cpu.idx:
                    return "cpu idxs differ"

                if config.qemu.ignore_unset_registers and c1cpu.registers_size != c2cpu.registers_size:
                    return f"cpu registers sizes differ: {c1cpu.registers_size} != {c2cpu.registers_size}"

                for reg_index in range(c1cpu.registers_size):
                    c1reg = c1cpu.registers[reg_index]
                    r1name = c1reg.fname(config.qemu.gdb_reg_map)

                    c2reg = c2cpu.registers[reg_index]
                    r2name = c2reg.fname(config.qemu.gdb_reg_map)

                    if r1name in config.qemu.ignore_registers or \
                        (config.qemu.ignore_unset_registers and not r1name in gdb_reg_map_values):
                        continue

                    if c1reg.size != c2reg.size:
                        return "reg sizes differ"

                    if r1name != r2name:
                        return f"reg names differ: {r1name} != {r2name}"

                    r1data = c1reg.fdata(config.qemu.endian)
                    r2data = c2reg.fdata(config.qemu.endian)
                    if r1data != r2data:
                        return f"reg data differ: ({r1name}:{r1data}) != ({r2name}:{r2data})"

            return None
        else:
            c1shm = c1.shm_struct.shm_tb
            c2shm = c2.shm_struct.shm_tb

            print(f"1: {format_tbinfo(c1shm.infos[c1shm.size-1])}\n2:{format_tbinfo(c2shm.infos[c2shm.size-1])}")


    while any(map(lambda c: c.is_open, clients)):
        # lockstepping
        for client in clients: 
            if client.is_open:
                try:
                    client.sem_client.release()

                    # wait at most 0.1 second, if an error occurs: assume that the client finished
                    client.sem_server.acquire(0.1)
                except ipc.BusyError:
                    logger.debug(f"run_lockstep: BusyError: noticed that client #{client.id} shutdown. marking as closed")
                    client.is_open = False
        
        print("comparing;;;")
        compare_res = compare_client_state()
        print("done;;;")
        if compare_res is not None:
            print("FAIL: ", compare_res)
            for client in clients:
                if client.process is not None:
                    client.process.terminate()
            return

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
        client.process = run_with_callback([executable_path, *args], callback, config, client)

    if config.testing.protocol.mode == 'lockstep':
        run_lockstep(config)

    # print("----- REPORT START -----")
    #
    # clients_disas = []
    # for client in clients:
    #     print(f"----- CLIENT #{client.id} START -----")
    #     disas = collect_disas(client, config)
    #     clients_disas.append(disas)
    #     print(disas)
    #     print(f"----- CLIENT #{client.id} END -----")
    #
    # first_disas = clients_disas[0]
    # all_equal = True
    # for disas in clients_disas[1:]:
    #     if first_disas != disas:
    #         all_equal = False
    #
    # print("----- REPORT SUMMARY -----")
    # if all_equal:
    #     print("Result: All clients returned the same output.")
    # else:
    #     print("Result: All clients returned the same output.")
    # print("----- REPORT SUMMARY -----")
    #
    # print("----- REPORT END -----")

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

def cleanup_client(config: Config, client: Client):
    cleanup_sem(config, client)
    cleanup_smh(config, client)

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
        logging.basicConfig(filename=config.logging.file, level=config.logging.level)
    else:
        logging.disable()

    if not config.dev.dry_run:
        atexit.register(cleanup, config)
        main(config)
    else:
        logger.info(f"Dry-Run. Config: {config}")

