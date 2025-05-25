from dataclasses import dataclass, field
import tomllib
from typing import Literal 
import dacite
from typing import TypeAlias

Endian: TypeAlias = Literal['little', 'big']

@dataclass
class Logging:
    level: str
    file: str = "cosim.json"
    dir: str = "./logs"
    enable: bool = True
    clear_on_rerun: bool = False

@dataclass
class Out:
    dir: str = "./result"
    format: Literal["json"] = "json"

@dataclass
class Protocol:
    mode: Literal["lockstep"]
    layer: Literal["tb", "insn"]
    execute_all_remaining_instructions: bool
    stop_after_n_instructions: int
    skip_n_instructions: int = 0
    out: Out = field(default_factory=Out)

@dataclass
class Testing:
    test_exec: str
    protocol: Protocol
    max_trace_length: int

@dataclass
class Client:
    exec: str
    additional_args: list[str]

@dataclass
class Qemu:
    plugin: str
    endian: Endian 
    clients: list[Client]
    gdb_reg_map: dict[str, str]
    ignore_registers: list[str]
    ignore_unset_registers: bool = True

@dataclass
class Dev:
    dry_run: bool

@dataclass
class Config:
    qemu: Qemu
    testing: Testing
    logging: Logging
    dev: Dev

def load_config(path: str) -> Config | None:
    with open(path, mode="rb") as config_file:
        data = tomllib.load(config_file)
        try:
            return dacite.from_dict(data_class=Config, data=data)
        except TypeError as e:
            print(f"Missing required field: {e}")
        except Exception as e:
            print(f"Error while loading config: {e}")

