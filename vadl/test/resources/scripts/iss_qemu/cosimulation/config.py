from dataclasses import dataclass, field
import tomllib
from typing import Literal 
import dacite

@dataclass
class Logging:
    level: str
    file: str = "cosim.log"
    enable: bool = True

@dataclass
class Out:
    dir: str = "./out"
    format: str = "json"

@dataclass
class Protocol:
    mode: Literal["lockstep"]
    layer: Literal["tb", "exec"]
    take_all_instructions: bool
    take_n_instructions: int
    skip_n_instructions: int = 0
    out: Out = field(default_factory=Out)

@dataclass
class Testing:
    test_exec: str
    protocol: Protocol

@dataclass
class Client:
    exec: str
    additional_args: list[str]

@dataclass
class Qemu:
    plugin: str
    endian: Literal["big", "little"] 
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

