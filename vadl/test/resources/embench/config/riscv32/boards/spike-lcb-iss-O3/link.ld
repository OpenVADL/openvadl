OUTPUT_ARCH("riscv")
ENTRY(_start)

SECTIONS
{
  /* Match the generated processor's reset PC. */
  . = 0x80000000;
  .text.init : { KEEP(*(.text.init .text.init.*)) }

  . = ALIGN(0x1000);
  .text.startup : { *(.text.startup .text.startup.*) }

  . = ALIGN(0x1000);
  .text : { *(.text .text.*) }

  /* Do not let orphan read-only data precede startup. */
  . = ALIGN(0x1000);
  .rodata : { *(.rodata .rodata.* .srodata .srodata.*) }

  . = ALIGN(0x1000);
  .data : { *(.data .data.* .sdata .sdata.*) }

  . = ALIGN(0x1000);
  .tohost : { KEEP(*(.tohost)) }
  .bss (NOLOAD) : { *(.bss .bss.* .sbss .sbss.*) *(COMMON) }

  . = ALIGN(0x1000);
  PROVIDE(__stack_size = 64K);
  PROVIDE(__stack_start = .);
  PROVIDE(__stack_shift = LOG2CEIL(ALIGN(__stack_size, 0x10)));
  __ehdr_start = .;
  _end = .;
}

