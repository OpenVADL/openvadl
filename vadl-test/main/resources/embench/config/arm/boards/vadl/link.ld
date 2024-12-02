ENTRY(_start)

MEMORY
{
  FLASH (rx) : ORIGIN = 0x80000000, LENGTH = 1M
  RAM (rwx)  : ORIGIN = 0x90000000, LENGTH = 16M
}

SECTIONS
{
    . = 0x80000000;
    .text.init : { *(.text.init) } > FLASH

    . = ALIGN(32);

    .text : {
        *(.text*)
        *(.rodata*)
    } > FLASH
    .data : { *(.data*) } > RAM
    /* This is currently not actually zero-initialized! has to be done by some crt */
    .bss : { *(.bss*) } > RAM

    . = ALIGN(16);
    .stack (NOLOAD) : {
        . = . + 8M;
        . = ALIGN(16);
        __stack_top = .;
    } > RAM
}
