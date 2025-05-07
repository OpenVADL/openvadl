"""
A small helper-script to generate type-hints for pythons c-type class-structs.
Insert the value of the _fields_-List in the fields variable below.
The generated __init__ method is printed to stdout, simply add it to the class-struct.
"""

import re
from typing import Optional

fields = '[("shm_tb", BrokerSHM_TB), ("shm_exec", BrokerSHM_Exec)]'

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

output = ''
output += 'def __init__(self, *args: Any, **kw: Any) -> None:\n'
output += '\tsuper().__init__(*args, **kw)\n'

for match in found:
    name = match[1].strip()
    typ = match[2].strip()
    arr_typ = array_typ(typ)
    if arr_typ:
        inner_typ = arr_typ[0]
        inner_size = arr_typ[1]
        if inner_typ == 'c_char':
            output += f'\tself.{name}: Annotated[bytes, {inner_typ} * self.{inner_size}]\n'
        else:
            ptyp = python_typ(inner_typ)
            output += f'\tself.{name}: Annotated[list[{ptyp}], {inner_typ} * self.{inner_size}]\n'
    else:
        ptyp = python_typ(typ)
        output += f'\tself.{name}: Annotated[{ptyp}, {typ}]\n'

print(output)
