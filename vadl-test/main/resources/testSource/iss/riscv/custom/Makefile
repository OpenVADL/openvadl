OUT_DIR=out
RESULTS_DIR=results
TESTS=$(wildcard tests/*.S)
TARGETS=$(TESTS:tests/%.S=$(OUT_DIR)/%.elf)

CC = riscv64-unknown-elf-gcc
AS = riscv64-unknown-elf-as
LD = riscv64-unknown-elf-ld
CPP = riscv64-unknown-elf-gcc -E

ARCH = rv64i_zicsr
ABI = lp64
ASFLAGS = -march=$(ARCH) -mabi=$(ABI)
LDFLAGS = -T link.ld

QEMU=qemu-system-rv64im
QMACHINE = virt
QFLAGS = -nographic -M $(QMACHINE)

PREPROCESSED=$(TESTS:tests/%.S=$(OUT_DIR)/%.preprocessed.S)

all: prepare $(TARGETS)

prepare:
	mkdir -p $(OUT_DIR) $(RESULTS_DIR)
	
$(OUT_DIR)/%.preprocessed.S: tests/%.S
	$(CPP) -I. $< -o $@

$(OUT_DIR)/%.o: $(OUT_DIR)/%.preprocessed.S
	$(AS) $(ASFLAGS) $< -o $@

$(OUT_DIR)/%.elf: $(OUT_DIR)/%.o link.ld
	$(LD) $(LDFLAGS) $< -o $@

build-%: prepare $(OUT_DIR)/%.elf
	
run-%: $(OUT_DIR)/%.elf
	$(QEMU) $(QFLAGS) -bios $<

clean:
	rm -rf $(OUT_DIR) $(RESULTS_DIR)

# Ensure preprocessed files are kept
keep-preprocessed: $(PREPROCESSED)

.PHONY: keep-preprocessed