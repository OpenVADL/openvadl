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
A small helper-script to generate type-hints for pythons c-type class-structs.
Insert the value of the _fields_-List in the fields variable below.
The generated __init__ method is printed to stdout, simply add it to the class-struct.
"""

import re
from typing import Optional
from dataclasses import dataclass
from pprint import pprint

fields = '[("size", c_size_t), ("buffer", c_uint8 * MAX_INSN_DATA_SIZE)]'

@dataclass
class CField:
    ctype: str
    name: str
    array_mod: Optional[str]

@dataclass
class CStruct:
    name: str
    cfields: list[CField]

def parse_cfield(input: str) -> CField:
    expr = re.compile(r"\s*(.*?)\s+(.*?)\s*(\[(.*?)\])?\s*;")
    found = expr.findall(input)[0]
    ctype = found[0]
    name = found[1]
    array_mod = found[3]
    if array_mod.strip() == "":
        array_mod = None
    cfield = CField(ctype, name, array_mod)
    return cfield

def parse_cstructs(input: str) -> list[CStruct]:
    expr = re.compile(r"\s*typedef\s*struct\s*{((.|\s)*?)}\s*(.*?)\s*;", re.MULTILINE)
    found = expr.findall(input)
    cstructs = []

    for f in found:
        cfields = filter(lambda s: len(s) > 0, f[0].split("\n"))
        cfields = list(map(parse_cfield, cfields))
        name = f[2]
        cstructs.append(CStruct(name, cfields))

    return cstructs

def into_python_str(cstruct: CStruct) -> str:
    out = ""

    for field in cstruct.cfields:
        if field.array_mod is not None:
            out += f"{field.array_mod} = <TODO>\n"

    out += "_fields_ = "

    def format_field(field: CField) -> str:
        if field.array_mod is not None:
            return f"(\"{field.name}\", {c_typ(field.ctype)} * {field.array_mod})" 
        else:
            return f"(\"{field.name}\", {c_typ(field.ctype)})"

    fs = ", ".join([format_field(f) for f in cstruct.cfields])
    out += f"[{fs}]\n\n"

    out += "def __init__(self, *args: Any, **kw: Any) -> None:\n"
    out += "\tsuper().__init__(*args, **kw)\n"

    def format_init_field(field: CField) -> str:
        if field.array_mod is not None:
            return f"\tself.{field.name}: Annotated[{python_array_typ(field.ctype)}, {c_typ(field.ctype)} * {field.array_mod}]"
        else:
            return f"\tself.{field.name}: Annotated[{python_typ(field.ctype)}, {c_typ(field.ctype)}]"

    fs = "\n".join([format_init_field(f) for f in cstruct.cfields])
    out += fs
    return out
        

def array_typ(typ: str) -> Optional[tuple[str, str]]:
    expr = re.compile(r"^(.*?)\*(.*?)$")
    found = expr.findall(typ)
    if len(found) > 0:
        f = found[0]
        return f[0].strip(), f[1].strip()
    else:
        return None


def c_typ(typ: str) -> str:
    if typ[0].isupper():
        return typ
    else:
        return f"c_{typ}"

def python_typ(typ: str) -> str:
    typ_map = {
        "uint64": "int",
        "int": "int",
        "uint": "int",
        "uint8": "int",
        "size_t": "int",
        "char": "bytes",
    }
    if typ in typ_map:
        return typ_map[typ]
    else:
        return typ

def python_array_typ(typ: str) -> str:
    ptyp = python_typ(typ)
    if ptyp == "bytes":
        return ptyp
    else:
        return f"list[{ptyp}]"

s = """
typedef struct {
  int init_mask;
  SHMCPU cpus[MAX_CPU_COUNT];
  TBInfo tb_info;
} BrokerSHM_TB;
"""

cstructs = parse_cstructs(s)
into_python = [into_python_str(cstruct) for cstruct in cstructs]
print("\n\n".join(into_python))

# expr = re.compile(r'(\("(.*?)",(.*?)\))')
# found = expr.findall(fields)
#
# output = ""
# output += "def __init__(self, *args: Any, **kw: Any) -> None:\n"
# output += "\tsuper().__init__(*args, **kw)\n"
#
# for match in found:
#     name = match[1].strip()
#     typ = match[2].strip()
#     arr_typ = array_typ(typ)
#     if arr_typ:
#         inner_typ = arr_typ[0]
#         inner_size = arr_typ[1]
#         if inner_typ == "c_char":
#             output += (
#                 f"\tself.{name}: Annotated[bytes, {inner_typ} * self.{inner_size}]\n"
#             )
#         else:
#             ptyp = python_typ(inner_typ)
#             output += f"\tself.{name}: Annotated[list[{ptyp}], {inner_typ} * self.{inner_size}]\n"
#     else:
#         ptyp = python_typ(typ)
#         output += f"\tself.{name}: Annotated[{ptyp}, {typ}]\n"
#
# print(output)
