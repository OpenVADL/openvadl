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
The following classes represent the equally defined c-structs in the cosimulation QEMU plugin.
They are used to transfer data from a QEMU-client to the broker using shared memory.
See `BrokerSHM(Structure)` as the "entrypoint" of this class-hierarchy.

Hint: Use the annotate-fields.py script to help generate parts of these python classes.
"""

from ctypes import Structure, Union, c_char, c_int, c_size_t, c_uint, c_uint8, c_uint64
from typing import Annotated, Any

SHMSTRING_MAX_LEN = 1024
TBINFO_ENTRIES = 1024
TBINSNINFO_ENTRIES = 64

MAX_REGISTER_NAME_SIZE = 256
MAX_REGISTER_DATA_SIZE = 256
MAX_CPU_REGISTERS = 256
MAX_CPU_COUNT = 8
MAX_INSN_DATA_SIZE = 64


class SHMString(Structure):
    _fields_ = [("len", c_size_t), ("value", c_char * SHMSTRING_MAX_LEN)]

    def __repr__(self):
        return f"SHMString(len={self.len}, value={self.fstr()})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self):
        return {"len": self.len, "value": self.fstr()}

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.len: Annotated[int, c_size_t]
        self.value: Annotated[bytes, c_char * SHMSTRING_MAX_LEN]

    def fstr(self) -> str:
        return self.value[: self.len].decode(errors="backslashreplace")


class InsnData(Structure):
    _fields_ = [("size", c_size_t), ("buffer", c_uint8 * MAX_INSN_DATA_SIZE)]

    def __repr__(self):
        return f"InsnData(size={self.size}, buffer={self.fbuffer()})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self):
        return {"size": self.size, "buffer": self.fbuffer()}

    def fbuffer(self) -> str:
        bytes_formatted = [num.to_bytes() for num in self.buffer[: self.size]]
        res = b"".join(reversed(bytes_formatted))
        return res.hex(" ")

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.size: Annotated[int, c_size_t]
        self.buffer: Annotated[list[int], c_uint8 * MAX_INSN_DATA_SIZE]


class TBInsnInfo(Structure):
    _fields_ = [
        ("pc", c_uint64),
        ("size", c_size_t),
        ("symbol", SHMString),
        ("hwaddr", SHMString),
        ("disas", SHMString),
        ("data", InsnData),
    ]

    def __repr__(self):
        return f"TBInsnInfo(pc={self.pc}, symbol={self.symbol}, hwaddr={self.hwaddr}, disas={self.disas}, data={self.data})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self):
        return {
            "pc": self.pc,
            "size": self.size,
            "symbol": self.symbol.to_dict(),
            "hwaddr": self.hwaddr.to_dict(),
            "disas": self.disas.to_dict(),
            "data": self.data.to_dict(),
        }

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.pc: Annotated[int, c_uint64]
        self.size: Annotated[int, c_size_t]
        self.symbol: Annotated[SHMString, SHMString]
        self.hwaddr: Annotated[SHMString, SHMString]
        self.disas: Annotated[SHMString, SHMString]
        self.data: Annotated[InsnData, InsnData]


class TBInfo(Structure):
    _fields_ = [
        ("pc", c_uint64),
        ("insns_info_size", c_size_t),
        ("insns_info", TBInsnInfo * TBINSNINFO_ENTRIES),
    ]

    def __repr__(self):
        return f"TBInfo(pc={self.pc}, insns_info_size={self.insns_info_size}, insns_info={self.insns_info[: self.insns_info_size]})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self):
        return {
            "pc": self.pc,
            "insns_info_size": self.insns_info_size,
            "insns_info": [
                insn.to_dict() for insn in self.insns_info[: self.insns_info_size]
            ],
        }

    def insns_info_list(self) -> list[TBInsnInfo]:
        return self.insns_info[: self.insns_info_size]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.pc: Annotated[int, c_uint64]
        self.insns_info_size: Annotated[int, c_size_t]
        self.insns_info: Annotated[list[TBInsnInfo], TBInsnInfo * TBINSNINFO_ENTRIES]


class SHMRegister(Structure):
    _fields_ = [
        ("size", c_int),
        ("data", c_uint8 * MAX_REGISTER_DATA_SIZE),
        ("name", SHMString),
    ]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.size: Annotated[int, c_int]
        self.data: Annotated[list[int], c_uint8 * MAX_REGISTER_DATA_SIZE]
        self.name: Annotated[SHMString, SHMString]

    def __repr__(self):
        return f"SHMRegister(size={self.size}, data={self.fdata()}, name={self.name})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, gdb_map: dict[str, str]):
        return {
            "size": self.size,
            "data": self.fdata(),
            "name": self.name.to_dict(),
            "name-mapped": self.fname(gdb_map),
        }

    def fname(self, gdb_map: dict[str, str]) -> str:
        n = self.name.fstr()  # assume that the name is "printable"
        if n in gdb_map:
            return gdb_map[n]
        else:
            return n

    def fdata(self) -> str:
        bytes_formatted = [num.to_bytes() for num in self.data[: self.size]]
        res = b"".join(reversed(bytes_formatted))
        return res.hex(" ")


class SHMCPU(Structure):
    _fields_ = [
        ("idx", c_uint),
        ("registers_size", c_size_t),
        ("registers", SHMRegister * MAX_CPU_REGISTERS),
    ]

    def __repr__(self):
        return f"SHMCPU(idx={self.idx}, registers_size={self.registers_size}, registers={self.registers[: self.registers_size]})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, gdb_map: dict[str, str]):
        return {
            "idx": self.idx,
            "registers_size": self.registers_size,
            "registers": [
                reg.to_dict(gdb_map) for reg in self.registers[: self.registers_size]
            ],
        }

    def registers_list(self) -> list[SHMRegister]:
        return self.registers[: self.registers_size]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.idx: Annotated[int, c_uint]
        self.registers_size: Annotated[int, c_size_t]
        self.registers: Annotated[list[SHMRegister], SHMRegister * MAX_CPU_REGISTERS]


class BrokerSHM_TB(Structure):
    _fields_ = [
        ("init_mask", c_int),
        ("cpus", SHMCPU * MAX_CPU_COUNT),
        ("tb_info", TBInfo),
    ]

    def __repr__(self):
        return f"BrokerSHM_TB(init_mask={self.init_mask}, cpus={self.cpus[:]}, tb_info={self.tb_info})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, gdb_map: dict[str, str]):
        return {
            "init_mask": self.init_mask,
            "cpus": [cpu.to_dict(gdb_map) for cpu in self.cpus[:]],
            "tb_info": self.tb_info.to_dict(),
        }

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.init_mask: Annotated[int, c_int]
        self.cpus: Annotated[list[SHMCPU], SHMCPU * MAX_CPU_COUNT]
        self.tb_info: Annotated[TBInfo, TBInfo]


class BrokerSHM_Exec(Structure):
    _fields_ = [
        ("init_mask", c_int),
        ("cpus", SHMCPU * MAX_CPU_COUNT),
        ("insn_info", TBInsnInfo),
    ]

    def __repr__(self):
        return f"BrokerSHM_Exec(init_mask={self.init_mask}, cpus={self.cpus[:]}, insn_info={self.insn_info})"

    def __format__(self, _: str, /) -> str:
        return self.__repr__()

    def to_dict(self, gdb_map: dict[str, str]):
        return {
            "init_mask": self.init_mask,
            "cpus": [cpu.to_dict(gdb_map) for cpu in self.cpus[:]],
            "insn_info": self.insn_info.to_dict(),
        }

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.init_mask: Annotated[int, c_int]
        self.cpus: Annotated[list[SHMCPU], SHMCPU * MAX_CPU_COUNT]
        self.insn_info: Annotated[TBInsnInfo, TBInsnInfo]


class BrokerSHM(Union):
    _fields_ = [("shm_tb", BrokerSHM_TB), ("shm_exec", BrokerSHM_Exec)]

    def __init__(self, *args: Any, **kw: Any) -> None:
        super().__init__(*args, **kw)
        self.shm_tb: Annotated[BrokerSHM_TB, BrokerSHM_TB]
        self.shm_exec: Annotated[BrokerSHM_Exec, BrokerSHM_Exec]
