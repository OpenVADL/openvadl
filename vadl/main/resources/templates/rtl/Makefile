# Makefile for [(${projectName})]

test: riscv-tests
	sbt test 2>&1 | tee build/[(${topModule})]-riscv-tests.log

riscv-tests:
	git clone https://github.com/riscv/riscv-tests
	cd riscv-tests && \
		git checkout b5ba87097c42aa41c56657e0ae049c2996e8d8d8 && \
		git submodule update --init --recursive
	# remove multicore disable because mhartid is not implemented
	sed -ie '/#define RISCV_MULTICORE_DISABLE/{n;N;d}' riscv-tests/env/p/riscv_test.h
	sed -ie '/csrr a0, mhartid;/d' riscv-tests/env/p/riscv_test.h
[# th:unless="${hasEcall}"]	# patch ecall to also write test result to tohost address
	sed -i '/ecall/i \
		la a4, tohost; \\\
		sw TESTNUM, 0(a4); \\' riscv-tests/env/p/riscv_test.h
[/]	cd riscv-tests && \
		autoconf && \
		./configure --prefix=/usr && \
		RISCV_GCC_OPTS="-static -mcmodel=medany -fvisibility=hidden -nostdlib -nostartfiles -I/usr/include" make isa
