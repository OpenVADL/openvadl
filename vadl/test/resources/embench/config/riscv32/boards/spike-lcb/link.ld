OUTPUT_ARCH( "riscv" )
ENTRY(_start)

/* https://github.com/riscv/riscv-test-env/blob/master/p/link.ld */
/* https://github.com/ucb-bar/libgloss-htif/blob/master/util/htif.ld */
SECTIONS
{
  . = 0xe0000000;
  .text.trap : { *(.text.trap) }

  . = 0x80000000;
  .text.init : { *(.text.init) }

  . = ALIGN(0x1000);
  .text.startup : { *(.text.startup) }

  . = ALIGN(0x1000);
  .text : { *(.text) }

  . = ALIGN(0x1000);
  .data : { *(.data) }
  .data.string : { *(.data.string)}

  . = ALIGN(0x1000);
  .tohost : { *(.tohost) }

  . = ALIGN(0x1000);
  /* place Thread Control Block (TCB) at bottom of hart stack */
  PROVIDE (__stack_size = 64K);
  PROVIDE (__stack_start = .);
  PROVIDE (__stack_shift = LOG2CEIL( ALIGN (__stack_size, 0x10)));
  __ehdr_start = .;  
  _end = .;
}
