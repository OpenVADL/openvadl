CC=clang
CFLAGS=-target aarch64-none-elf -march=armv8-a+nofp+nosimd -O3
CPPFLAGS=-I /opt/aarch64-cross/include -D__SOFTFP__

# These use xf_float:
SRCS_EXCLUDE=divxc3.c \
	fixunsxfdi.c \
	fixunsxfsi.c \
	fixunsxfti.c \
	fixxfdi.c \
	fixxfti.c \
	floatdixf.c \
	floattixf.c \
	floatundixf.c \
	floatuntixf.c \
	mulxc3.c \
	powixf2.c

SRCS = $(filter-out $(SRCS_EXCLUDE), $(wildcard *.c))

OBJS=$(SRCS:.c=.o)

libllvm-builtins.a: $(OBJS)
	ar rcs $@ $^
	ranlib $@

install: libllvm-builtins.a
	mkdir -p $(PREFIX)/lib
	cp -p libllvm-builtins.a $(PREFIX)/lib/libllvm-builtins.a

.PHONY:
clean:
	rm -rf libllvm-builtins.a *.o