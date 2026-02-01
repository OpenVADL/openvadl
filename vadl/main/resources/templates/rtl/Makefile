# Makefile for [(${projectName})]

# Preserve test exit code when piping
SHELL=/bin/bash -o pipefail

.PHONY: test

test:
	sbt "testOnly ElfSimTest"

# Elfsim with C++ memory model
SIM_BUILD=build/elfsim
SIM_WORK=$(SIM_BUILD)/workdir-verilator
SIM=$(SIM_WORK)/simulation
SIM_LOG=$(SIM_BUILD)/simulation.log
SIM_RPT=$(SIM_BUILD)/simulation.rpt

TESTS?=$(filter-out %.dump,$(wildcard /riscv-tests/isa/[(${#strings.substring(isaName, 0, 4).toLowerCase()})]ui-p-*))
TEST_OUT=$(SIM_BUILD)/tests

$(SIM):
	sbt "test"

$(TEST_OUT): $(SIM)
	mkdir -p $(TEST_OUT)

$(SIM_RPT): $(SIM) $(TESTS) $(TEST_OUT)
	(for t in $(TESTS); do echo "test $$t"; \
		echo -e "R a\nS 1 0\nR a\nS 1 1\nT 0 0,1-5*1\nS 1 0\nR 0\nW 1\nT 0 0,1-5*ffff\nD" \
		| ($(SIM) "+elf=$$t" 2>&1 || true) | tee $(TEST_OUT)/`basename $$t`.log ; \
		mv trace.vcd $(TEST_OUT)/`basename $$t`.vcd; done) \
		| tee $(SIM_LOG)
	grep -E "^(test|^RVTEST_)" $(SIM_LOG) | tee $(SIM_RPT)

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
