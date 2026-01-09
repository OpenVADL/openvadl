import os
import subprocess
from pathlib import Path

AS_BE = "powerpc64-unknown-elf-as"
LD_BE = "powerpc64-unknown-elf-ld"
OBJDUMP_BE = "powerpc64-unknown-elf-objdump"
AS_LE = "powerpc64le-unknown-elf-as"
LD_LE = "powerpc64le-unknown-elf-ld"
OBJDUMP_LE = "powerpc64le-unknown-elf-objdump"

def compile(id: str, asm: str, debug: bool = True) -> dict:
  asm_path = build_assembly(id, asm)
  linker_path = build_linker_script(id)

  # big-endian
  obj_be = _tmp_file(id, f"obj-{id}-be.o")
  elf_be = _tmp_file(id, f"elf-{id}-be")
  assemble(AS_BE, asm_path, obj_be)
  link(LD_BE, linker_path, obj_be, elf_be)

  # little-endian
  obj_le = _tmp_file(id, f"obj-{id}-le.o")
  elf_le = _tmp_file(id, f"elf-{id}-le")
  assemble(AS_LE, asm_path, obj_le)
  link(LD_LE, linker_path, obj_le, elf_le)

  result = {
    "asm": asm_path,
    "lnscript": linker_path,
    "obj_be": obj_be,
    "elf_be": elf_be,
    "obj_le": obj_le,
    "elf_le": elf_le,
  }

  if debug:
    objdump_be = _tmp_file(id, f"elf-{id}-be.dump")
    objdump_le = _tmp_file(id, f"elf-{id}-le.dump")
    objdump(OBJDUMP_BE, elf_be, objdump_be)
    objdump(OBJDUMP_BE, elf_le, objdump_le)
    result.update({
        "objdump_be": objdump_be,
        "objdump_le": objdump_le,
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

  content = f"""
  .machine power10
  .globl _start
  .section .text
  _start:
  nop
  {core}
  ba 0xfc
  .section .data
  ba 0x104
  """
  with open(asm_out, "w") as f:
    f.write(content)
  return asm_out


def build_linker_script(id: str) -> Path:
  linker_out = _tmp_file(id, f"linker-{id}.ld")

  content = """
  ENTRY(_start)
  SECTIONS
  {
      . = 0x00000000000000FC;
      .text : { *(.text) }
      . = 0x0000000000000700;
      .data : { *(.data) }
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
