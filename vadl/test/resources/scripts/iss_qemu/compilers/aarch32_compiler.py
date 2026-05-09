import asyncio
import os
from pathlib import Path

CC="arm-linux-gnueabihf-gcc"
NM="arm-linux-gnueabihf-nm"

async def compile(id: str, asm: str, compargs: str) -> dict:
  asm_path = await build_assembly(id, asm)
  linker_path = await build_linker_script(id)

  elf_out = _tmp_file(id, f"elf-{id}")

  # Find tohost symbol address.
  proc = await asyncio.create_subprocess_exec(
    CC, *compargs.split(), "-T", linker_path, "-nostartfiles", "-o", elf_out, asm_path,
    stdout=asyncio.subprocess.PIPE,
    stderr=asyncio.subprocess.PIPE
  )
  stdout, stderr = await proc.communicate()
  if proc.returncode != 0:
    raise RuntimeError(f"Compilation failed: {stderr.decode()}")

  # Find the address of 'tohost' symbol
  proc = await asyncio.create_subprocess_exec(
    NM, elf_out,
    stdout=asyncio.subprocess.PIPE,
    stderr=asyncio.subprocess.PIPE
  )
  stdout, _ = await proc.communicate()
  tohost_addr = None
  for line in stdout.decode().splitlines():
    if " tohost" in line:
      tohost_addr = int(line.split()[0], 16)
      break

  return {
    "elf": elf_out,
    "asm": asm_path,
    "lnscript": linker_path,
    "tohost_addr": tohost_addr,
  }


async def build_assembly(id: str, core: str) -> Path:
  asm_out = _tmp_file(id, f"asm-{id}.s")

  content = f"""
  .section .text.init
  .global _start
  
  .arm                         @ only A32, no T32
  .syntax unified
  
  _start:
      {core}

  @ TODO: exit VADL virt (HTIF)
  @ TODO: exit upstream virt (semihosting)

  1:  b       1b               @ Infinite loop
  """
  with open(asm_out, "w") as f:
    f.write(content)
  return asm_out


async def build_linker_script(id: str) -> Path:
  linker_out = _tmp_file(id, f"linker-{id}.ld")

  content = """
  OUTPUT_ARCH("arm")
  ENTRY(_start)

  SECTIONS
  {
    . = 0x40000000;
    .text.init : { *(.text.init) }
    . = ALIGN(0x1000);
    .tohost : { *(.tohost) }
    . = ALIGN(0x1000);
    .text : { *(.text) }
    . = ALIGN(0x1000);
    .data : { *(.data) }
    .bss : { *(.bss) }
    _end = .;
  }
  """
  with open(linker_out, "w") as f:
    f.write(content)
  return linker_out

def _tmp_file(id: str, name: str) -> Path:
  build_dir = f"/tmp/build-{id}/"
  os.makedirs(build_dir, exist_ok=True)
  return Path(f"{build_dir}/{name}")

