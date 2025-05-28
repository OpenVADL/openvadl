OUTPUT_ARCH("aarch64")
ENTRY(_start)

SECTIONS
{
  . = 0x40000000; /* RAM start */
  .text.init : { *(.text.init) }
  . = ALIGN(0x1000);
  .tohost : { *(.tohost) }
  . = ALIGN(0x1000);
  .text : { *(.text) }
  . = ALIGN(0x1000);
  .data : { *(.data) }
  .bss : { *(.bss) }
  . = ALIGN(16);
  . = . + 8M;
  . = ALIGN(16);
  __stack_top = .;
  _end = .;
}