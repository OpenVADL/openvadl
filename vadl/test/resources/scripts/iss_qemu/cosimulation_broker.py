from ctypes import POINTER, c_char, c_char_p, c_uint64, c_void_p, sizeof, Structure, c_int, c_size_t
from dataclasses import dataclass
import argparse
import sys
import mmap
import subprocess
import posix_ipc as ipc
import atexit

import logging
from typing import Optional

reference_ricsv64_exe = "/qemu-system-riscv64"

@dataclass
class Options:
    qemu_dir: str
    clients: int
    test_exec: str

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

class TBInsnInfo(Structure):
    _fields_ = [("pc", c_uint64), ("size", c_size_t), ("symbol", SHMString), ("hwaddr", SHMString), ("disas", SHMString)]

    def __repr__(self):
        return f"<{self.__class__.__name__}: pc={self.pc}, size={self.size}, symbol={self.symbol}, hwaddr={self.hwaddr}, disas={self.disas}>"

    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}

class TBInfo(Structure):
    INSNS_INFOS_SIZE = 32
    _fields_ = [("pc", c_uint64), ("insns", c_size_t), ("insns_info_size", c_size_t), ("insns_info", TBInsnInfo * INSNS_INFOS_SIZE)]

    def __repr__(self):
        values = f"pc={self.pc}, insns={self.insns}, insns_info_size={self.insns_info_size}, insns_info={self.insns_info[:self.insns_info_size]}"
        return f"<{self.__class__.__name__}: {values}>"
    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}


class SHMStruct(Structure):
    INFOS_SIZE = 1024
    _fields_ = [("size", c_size_t), ("infos", TBInfo * INFOS_SIZE)]

    def __repr__(self):
        values = f"size={self.size}, infos={self.infos[:self.size]}"
        return f"<{self.__class__.__name__}: {values}>"
    
    def _asdict(self):
        return {field[0]: getattr(self, field[0]) 
                for field in self._fields_}

def format_tb_insn_info(insn: TBInsnInfo) -> str:
    _fields_ = [("pc", c_uint64), ("size", c_size_t), ("symbol", c_char_p), ("hwaddr", c_void_p), ("disas", c_char_p)]
    return f"TBInsn"

def format_tb_info(tb: TBInfo):
    pass

def format_shm(shm: SHMStruct):
    pass

@dataclass
class Client:
    id: int
    shm: ipc.SharedMemory
    shm_struct: SHMStruct
    sem_server: ipc.Semaphore
    sem_client: ipc.Semaphore
    is_open: bool
    # process: subprocess.Popen[bytes]

clients: list[Client] = []

import threading
import subprocess

def run_with_callback(command, on_complete, options: Options, client: Client):
    def runner():
        process = subprocess.Popen(command)
        process.wait()
        on_complete(process.returncode, options, client)
    
    thread = threading.Thread(target=runner)
    thread.start()
    return thread

# Usage
def callback(returncode: int, options: Options, client: Client):
    print(f"Process (client: {client.id}) finished with code: {returncode}")
    # keep smh alive for reporting 
    cleanup_sem(options, client)

# reading is possible if a client is done
def read(client: Client) -> Optional[TBInfo]:
    client.sem_server.acquire()
    smh = client.shm_struct
    for info in smh.infos:
        for insn in info.insns_info:
            if insn.pc > 0:
                logger.info(f"Reading client #{client.id}: {insn.disas.value.decode()}")

    client.sem_client.release()

    return None

def collect_disas(client: Client) -> str:
    disas = []
    smh = client.shm_struct
    for info in smh.infos:
        for insn in info.insns_info:
            if insn.pc > 0:
                disas.append(insn.disas.value.decode())
    return "\n".join(disas)

def main(options: Options):
    logger.debug(f"starting broker: options={options}")

    # create shared memory and semaphores per client
    for i in range(options.clients):
        shm = ipc.SharedMemory(f"/cosimulation-shm-{i}", ipc.O_CREX, size=sizeof(SHMStruct))
        mm = mmap.mmap(shm.fd, sizeof(SHMStruct))
        shm_struct = SHMStruct.from_buffer(mm) 

        sem_server = ipc.Semaphore(f"/cosimulation-sem-server-{i}", ipc.O_CREX)
        sem_client = ipc.Semaphore(f"/cosimulation-sem-client-{i}", ipc.O_CREX)
        logger.info(f"created shm and sems, spawning client with id: {i}")

        executable_path = f"{options.qemu_dir}/{reference_ricsv64_exe}"
        plugin_path = f"{options.qemu_dir}/contrib/plugins/libcosimulation.so,client-id={i}"
        args = ["-M", "spike", "-nographic", "-bios", options.test_exec, "-plugin", plugin_path]
        logger.info(f"starting client: {" ".join([executable_path, *args])}")
        client = Client(i, shm, shm_struct, sem_server=sem_server, sem_client=sem_client, is_open=True)
        clients.append(client)
        run_with_callback([executable_path, *args], callback, options, client)



    while any(map(lambda c: c.is_open, clients)):
        # lockstepping
        for client in clients: 
            if client.is_open:
                try:
                    client.sem_client.release() # start client
                    read(client)                # read from client[0] after done
                except ipc.ExistentialError: # something was closed, mark client as done
                    client.is_open = False

    print("----- REPORT START -----")

    clients_disas = []
    for client in clients:
        print(f"----- CLIENT #{client.id} START -----")
        disas = collect_disas(client)
        clients_disas.append(disas)
        print(disas)
        print(f"----- CLIENT #{client.id} END -----")

    first_disas = clients_disas[0]
    all_equal = True
    for disas in clients_disas[1:]:
        if first_disas != disas:
            all_equal = False

    print("----- REPORT SUMMARY -----")
    if all_equal:
        print("Result: All clients returned the same output.")
    else:
        print("Result: All clients returned the same output.")
    print("----- REPORT SUMMARY -----")

    print("----- REPORT END -----")

def cleanup_sem(options: Options, client: Client):
    try:
        client.sem_client.unlink()
        client.sem_server.unlink()
        client.sem_client.close()
        client.sem_server.close()
    except ipc.ExistentialError:
        pass # ignore

def cleanup_smh(options: Options, client: Client):
    try:
        client.shm.close_fd()
        client.shm.unlink()
    except ipc.ExistentialError:
        pass # ignore

def cleanup_client(options: Options, client: Client):
    cleanup_sem(options, client)
    cleanup_smh(options, client)

def cleanup(options: Options):
    logger.info("cleaning up shm and sems for each client")
    for client in clients:
        cleanup_client(options, client)



if __name__ == '__main__':
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler(sys.stdout)
    logger.addHandler(handler)

    parser = argparse.ArgumentParser(
        prog="Cosimulation Broker",
        description="Executes two (or more) qemu-instances in parallel which need to use the cosimulation plugin to connect to the broker."
    )
    parser.add_argument('--qemu-dir', type=str, help="Directory of the compiled qemu project", default="./build")
    parser.add_argument('-c', '--clients', type=int, help="The number of qemu clients to handle", default=2)
    parser.add_argument('--test-exec', type=str, help="The compiled executable that should be used as the input for each qemu instance.", required=True)
    args = parser.parse_args()
    options = Options(
        qemu_dir=args.qemu_dir,
        clients=args.clients,
        test_exec=args.test_exec
    )

    atexit.register(cleanup, options)

    main(options)

