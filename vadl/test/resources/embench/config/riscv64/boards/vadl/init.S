.section .sdata,"aw"
.global	begin_signature
.size 	begin_signature,4

begin_signature:
	.word 	0xFFFFFFFF

.global	end_signature
.size 	end_signature,4

end_signature:
	.word 5

.section .text.init;
.align  4;

.extern  main
.type    main, @function

.extern  _halt
.type    _halt, @function


.global _start
.type   _start, @function
_start:
    # startup
    lui      sp, %hi(0xf0000000)
    addi sp, sp, %lo(0xf0000000)
    # invoke
    call main
    
    la a1, begin_signature 
    sw a0, 0(a1)
    # teardown
    call _halt
