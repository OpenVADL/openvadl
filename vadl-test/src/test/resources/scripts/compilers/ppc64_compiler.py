import asyncio
import os
from pathlib import Path

AS_BE = "powerpc64-unknown-elf-as"
LD_BE = "powerpc64-unknown-elf-ld"
AS_LE = "powerpc64le-unknown-elf-as"
LD_LE = "powerpc64le-unknown-elf-ld"

async def compile(id: str, asm: str) -> dict:
  asm_path = await build_assembly(id, asm)
  linker_path = await build_linker_script(id)

  elf_out = _tmp_file(id, f"elf-{id}")

  # big-endian
  obj_be = _tmp_file(id, f"obj-{id}-be.o")
  elf_be = _tmp_file(id, f"elf-{id}-be")
  await assemble(AS_BE, asm_path, obj_be)
  await link(LD_BE, linker_path, obj_be, elf_be)

  # little-endian
  obj_le = _tmp_file(id, f"obj-{id}-le.o")
  elf_le = _tmp_file(id, f"elf-{id}-le")
  await assemble(AS_LE, asm_path, obj_le)
  await link(LD_LE, linker_path, obj_le, elf_le)

  return {
      "asm": asm_path,
      "lnscript": linker_path,
      "obj_be": obj_be,
      "elf_be": elf_be,
      "obj_le": obj_le,
      "elf_le": elf_le,
  }


async def assemble(as_cmd: str, asm_path: Path, obj_out: Path) -> None:
    proc = await asyncio.create_subprocess_exec(
        as_cmd, "-o", str(obj_out), str(asm_path),
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    _, stderr = await proc.communicate()
    if proc.returncode != 0:
        raise RuntimeError(f"Assembly failed ({as_cmd}): {stderr.decode()}")

async def link(ld_cmd: str, linker_script: Path, obj_in: Path, elf_out: Path) -> None:
    proc = await asyncio.create_subprocess_exec(
        ld_cmd, "-T", str(linker_script), "-o", str(elf_out), str(obj_in),
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    _, stderr = await proc.communicate()
    if proc.returncode != 0:
        raise RuntimeError(f"Linking failed ({ld_cmd}): {stderr.decode()}")

async def build_assembly(id: str, core: str) -> Path:
  asm_out = _tmp_file(id, f"asm-{id}.s")

  content = f"""
  .globl _start
  .section .text
  _start:
  nop
  {core}
  ba 0xfc
  """
  with open(asm_out, "w") as f:
    f.write(content)
  return asm_out


async def build_linker_script(id: str) -> Path:
  linker_out = _tmp_file(id, f"linker-{id}.ld")

  content = """
  ENTRY(_start)
  SECTIONS
  {
      . = 0x00000000000000FC;
      .text : { *(.text) }
  }
  """
  with open(linker_out, "w") as f:
    f.write(content)
  return linker_out

def _tmp_file(id: str, name: str) -> Path:
  build_dir = f"/tmp/build-{id}/"
  os.makedirs(build_dir, exist_ok=True)
  return Path(f"{build_dir}/{name}")
