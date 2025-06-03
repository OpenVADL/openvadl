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

fields = '[("size", c_size_t), ("buffer", c_uint8 * MAX_INSN_DATA_SIZE)]'


def array_typ(typ: str) -> Optional[tuple[str, str]]:
    expr = re.compile(r"^(.*?)\*(.*?)$")
    found = expr.findall(typ)
    if len(found) > 0:
        f = found[0]
        return f[0].strip(), f[1].strip()
    else:
        return None


def python_typ(typ: str) -> str:
    typ_map = {
        "c_uint64": "int",
        "c_int": "int",
        "c_uint": "int",
        "c_uint8": "int",
        "c_size_t": "int",
        "c_char": "str",
    }
    if typ.startswith("c_"):
        return typ_map[typ]
    else:
        return typ


expr = re.compile(r'(\("(.*?)",(.*?)\))')
found = expr.findall(fields)

output = ""
output += "def __init__(self, *args: Any, **kw: Any) -> None:\n"
output += "\tsuper().__init__(*args, **kw)\n"

for match in found:
    name = match[1].strip()
    typ = match[2].strip()
    arr_typ = array_typ(typ)
    if arr_typ:
        inner_typ = arr_typ[0]
        inner_size = arr_typ[1]
        if inner_typ == "c_char":
            output += (
                f"\tself.{name}: Annotated[bytes, {inner_typ} * self.{inner_size}]\n"
            )
        else:
            ptyp = python_typ(inner_typ)
            output += f"\tself.{name}: Annotated[list[{ptyp}], {inner_typ} * self.{inner_size}]\n"
    else:
        ptyp = python_typ(typ)
        output += f"\tself.{name}: Annotated[{ptyp}, {typ}]\n"

print(output)
