OUTPUT_ARCH( "riscv" )
ENTRY(_start)

SECTIONS
{
  . = 0x80000000;
  .text.init : { *(.text.init) }

  . = 0xe0000000;
  .text.trap : AT( ADDR(.text.init) + SIZEOF(.text.init) ) { *(.text.trap) }

  . = ALIGN(0x1000);
  .tohost : { *(.tohost) }

  . = ALIGN(0x1000);
  .text : { *(.text) }

  . = ALIGN(0x1000);
  .data : { *(.data) }
  .data.string : { *(.data.string)}
  .bss : { *(.bss) }
  .rodata : { *(.rodata) }
  __ehdr_start = .;
  _end = .;
}
