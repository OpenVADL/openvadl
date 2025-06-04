import asyncio
import os
from pathlib import Path

CC="aarch64-none-linux-gnu-gcc"
NM="aarch64-none-linux-gnu-nm"

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
  _start:
      {core}

  exit:
      # exit VADL virt (HTIF)
      mov     x0, #1           // cmd: exit 0
      ldr     x1, =tohost      // Load address of 'tohost' into x1
      str     x0, [x1]         // Store a0 to 'tohost'

      # exit upstream virt (semihosting)
      ldr     x1, =args
      mov     w0, #0x20        // SYS_EXIT operation
      hlt     #0xF000          // Trigger semihosting call

  1:  b       1b               // Infinite loop

  .section .data
  .align 3
  args:
  .xword 0x20026     // ADP_Stopped_ApplicationExit command
  .xword 0x0         // Exit Code (0)

  .section .tohost, "aw", @progbits
  .align 6
  .global tohost
  tohost: .xword 0
  .size tohost, 8

  .align 6
  .global fromhost
  fromhost: .xword 0
  .size fromhost, 8
  """
  with open(asm_out, "w") as f:
    f.write(content)
  return asm_out


async def build_linker_script(id: str) -> Path:
  linker_out = _tmp_file(id, f"linker-{id}.ld")

  content = """
  OUTPUT_ARCH("aarch64")
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

