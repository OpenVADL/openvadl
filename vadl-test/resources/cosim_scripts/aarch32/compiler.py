import os
import subprocess
from pathlib import Path

AS = "arm-none-linux-gnueabihf-as"
LD = "arm-none-linux-gnueabihf-ld"
OBJDUMP = "arm-none-linux-gnueabihf-objdump"

def compile(id: str, asm: str, debug: bool = True) -> dict:
    asm_path = build_assembly(id, asm)
    linker_path = build_linker_script(id)

    obj = _tmp_file(id, f"obj-{id}.o")
    elf = _tmp_file(id, f"elf-{id}")
    assemble(AS, asm_path, obj)
    link(LD, linker_path, obj, elf)

    result = {
        "asm": asm_path,
        "lnscript": linker_path,
        "obj": obj,
        "elf": elf
    }

    if debug:
        objdump_file = _tmp_file(id, f"elf-{id}.dump")
        objdump(OBJDUMP, elf, objdump_file)
        result.update({
            "objdump": objdump_file
        })

    return result


def assemble(as_cmd: str, asm_path: Path, obj_out: Path) -> None:
    proc = subprocess.run([
        as_cmd, "-o", str(obj_out), str(asm_path)],
    )
    if proc.returncode != 0:
        raise RuntimeError(f"Assembly failed ({as_cmd}): {proc.stderr.decode()}")

def link(ld_cmd: str, linker_script: Path, obj_in: Path, elf_out: Path) -> None:
    proc = subprocess.run([
        ld_cmd, "-T", str(linker_script), "-o", str(elf_out), str(obj_in)],
    )
    if proc.returncode != 0:
        raise RuntimeError(f"Linking failed ({ld_cmd}): {proc.stderr.decode()}")

def build_assembly(id: str, core: str) -> Path:
    asm_out = _tmp_file(id, f"asm-{id}.s")

    # We load the tests into the RAM region at 0x40000000, because loading into the
    # firmware region does not work, because aarch32/virt.vadl already has firmware.

    content = f"""
    .globl _start
    .section .text
    _start:
    ldr pc, =0x40000008
    nop
    {core}
    @ Jump triggers simulation termination (stoptrigger plugin)
    ldr pc, =0x40000004
    """
    with open(asm_out, "w") as f:
        f.write(content)
    return asm_out


def build_linker_script(id: str) -> Path:
    linker_out = _tmp_file(id, f"linker-{id}.ld")

    content = """
    ENTRY(_start)

    PHDRS
    {
      text_seg  PT_LOAD FLAGS(5);   /* R + X = 4 + 1 */
    }

    SECTIONS
    {
        . = 0x40000000;
        .text : { *(.text) }  :text_seg
    }
    """
    with open(linker_out, "w") as f:
        f.write(content)
    return linker_out

def objdump(objdump_bin: str, obj_file: Path, out_file: Path):
    with out_file.open("wb") as f:
      proc = subprocess.run(
          [objdump_bin, "-D", str(obj_file)],
          stdout=f,
      )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.decode())

def _tmp_file(id: str, name: str) -> Path:
    build_dir = f"/tmp/build-{id}/"
    os.makedirs(build_dir, exist_ok=True)
    return Path(f"{build_dir}/{name}")
