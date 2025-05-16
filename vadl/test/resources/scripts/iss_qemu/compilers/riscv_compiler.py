import asyncio
import os
from pathlib import Path

CC="riscv64-unknown-elf-gcc"
NM = "riscv64-unknown-elf-nm"

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
  .global _start
  .section .text.init

  _start:	
  {core}

  # shutdown simulation with exit(0)
  addi x1, x0, 1
  la x2, tohost
  sd x1, 0(x2)
  
  1: j 1b # Loop to avoid running off into invalid memory
  
  .section .tohost, "aw", @progbits
  .align 6; 
  .global tohost; 
  tohost: .dword 0; 
  .size tohost, 8;    
  .align 6; 
  .global fromhost; 
  fromhost: .dword 0; 
  .size fromhost, 8;
  """
  with open(asm_out, "w") as f:
    f.write(content)
  return asm_out


async def build_linker_script(id: str) -> Path:
  linker_out = _tmp_file(id, f"linker-{id}.ld")

  content = """
  OUTPUT_ARCH( "riscv" )
  ENTRY(_start)
  
  SECTIONS
  {
    . = 0x80000000;
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
