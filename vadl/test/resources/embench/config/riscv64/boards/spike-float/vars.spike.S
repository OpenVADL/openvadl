.pushsection .tohost,"aw",@progbits;
.align 6; .global tohost; tohost: .dword 0; .size tohost, 8;
.align 6; .global fromhost; fromhost: .dword 0; .size fromhost, 8;
.popsection;
