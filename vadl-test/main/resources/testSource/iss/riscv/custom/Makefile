OUT_DIR=out
RESULTS_DIR=results
TESTS=$(wildcard tests/*.S)
TARGETS=$(TESTS:tests/%.S=$(OUT_DIR)/%.elf)

CC = riscv64-unknown-elf-gcc
AS = riscv64-unknown-elf-as
LD = riscv64-unknown-elf-ld

ARCH = rv64i_zicsr
ABI = lp64
ASFLAGS = -march=$(ARCH) -mabi=$(ABI)
LDFLAGS = -T link.ld

QEMU=qemu-system-vadl

QMACHINE = virt
QFLAGS = -nographic -M $(QMACHINE)

all: prepare $(TARGETS)

prepare:
	mkdir -p $(OUT_DIR) $(RESULTS_DIR)
	
$(OUT_DIR)/%.o: tests/%.S
	$(AS) $(ASFLAGS) $< -o $@

$(OUT_DIR)/%.elf: $(OUT_DIR)/%.o link.ld
	$(LD) $(LDFLAGS) $< -o $@

build-%: prepare $(OUT_DIR)/%.elf
	
run-%: $(OUT_DIR)/%.elf
	$(QEMU) $(QFLAGS) -bios $<

clean:
	rm -rf $(OUT_DIR) $(RESULTS_DIR)