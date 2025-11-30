# Makefile for [(${projectName})]

test: riscv-tests
	mkdir -p build
	touch build/[(${topModule})]-riscv-tests.log
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

# scalafmt download and format target
ifeq ($(OS),Windows_NT)
	SCALAFMT_ARCH = x86_64
	SCALAFMT_OS = pc-win32
	SCALAFMT_EXT = .exe
else
    UNAME_P := $(shell uname -p)
	SCALAFMT_ARCH = x86_64
    ifneq ($(filter arm%,$(UNAME_P)),)
		SCALAFMT_ARCH = aarch64
    endif
    UNAME_S := $(shell uname -s)
    ifeq ($(UNAME_S),Linux)
		SCALAFMT_OS = pc-linux
    endif
    ifeq ($(UNAME_S),Darwin)
		SCALAFMT_OS = apple-darwin
    endif
endif

SCALAFMT_VER = 3.9.10
SCALAFMT_FILE = scalafmt-$(SCALAFMT_ARCH)-$(SCALAFMT_OS).zip
SCALAFMT_URL = https://github.com/scalameta/scalafmt/releases/download/v$(SCALAFMT_VER)/$(SCALAFMT_FILE)

.scalafmt/$(SCALAFMT_FILE):
	mkdir -p $@
	curl -L --output $@ "$(SCALAFMT_URL)"
.scalafmt/scalafmt$(SCALAFMT_EXT): .scalafmt/$(SCALAFMT_FILE)
	unzip -od .scalafmt/ $<

format: .scalafmt/scalafmt$(SCALAFMT_EXT)
	$< [# th:each="file : ${scalaFiles}"]\
		[(${file})][/]
