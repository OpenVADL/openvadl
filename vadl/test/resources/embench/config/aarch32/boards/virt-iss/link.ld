OUTPUT_ARCH("arm")
ENTRY(_start)

SECTIONS
{
  /* The AArch32 virt firmware jumps here, so place the image at the start of
     RAM instead of trying to own the firmware region at 0x0. */
  . = 0x40000000;
  .text.init : { *(.text.init) }

  /* Keep the HTIF mailbox on its own page so the device-facing symbol has a
     stable address and stays visually separate from ordinary program text. */
  . = ALIGN(0x1000);
  .tohost : { *(.tohost) }

  /* The remaining sections follow as a simple flat freestanding image. */
  . = ALIGN(0x1000);
  .text : { *(.text) }
  . = ALIGN(0x1000);
  .data : { *(.data) }
  .bss : { *(.bss) }

  /* Embench only needs a single process-style stack, so reserve one fixed
     stack block above the image and export its top for init.S. */
  . = ALIGN(16);
  . = . + 8M;
  . = ALIGN(16);
  __stack_top = .;
  _end = .;
}
