OUT_DIR=out
RESULTS_DIR=results
TESTS=$(wildcard tests/*.S)
TARGETS=$(TESTS:tests/%.S=$(OUT_DIR)/%.elf)

CC = gcc
AS = as
LD = ld
OBJCPY = objcopy
CPP = gcc -E

ASFLAGS = -march=armv8-a+sve2
LDFLAGS = -T link.ld

QEMU := $(shell command -v qemu-system-a64)
QEMU_DIR := $(dir $(QEMU))

QMACHINE = virt
QFLAGS = -nographic -M $(QMACHINE)

PREPROCESSED=$(TESTS:tests/%.S=$(OUT_DIR)/%.preprocessed.S)
ENDSTATE_PLUGIN := $(QEMU_DIR)/tests/tcg/plugins/libendstate.so

all: prepare $(TARGETS)

prepare:
	mkdir -p $(OUT_DIR) $(RESULTS_DIR)

$(OUT_DIR)/%.preprocessed.S: tests/%.S
	$(CPP) -I. $< -o $@

$(OUT_DIR)/%.o: $(OUT_DIR)/%.preprocessed.S
	$(AS) $(ASFLAGS) $< -o $@

$(OUT_DIR)/%.elf: $(OUT_DIR)/%.o link.ld
	$(LD) $(LDFLAGS) $< -o $@

$(OUT_DIR)/%.bin: $(OUT_DIR)/%.elf
	$(OBJCPY) -O binary $< $@

dump-%: $(OUT_DIR)/%.elf
	objdump -d $< > $(OUT_DIR)/$*.dump

build-%: prepare $(OUT_DIR)/%.bin dump-%
	@:

run-%: $(OUT_DIR)/%.elf
	$(QEMU) $(QFLAGS) \
	  -plugin $(ENDSTATE_PLUGIN),tohost=$(shell nm $< | awk '$$3=="tohost"{print $$1}') \
	  -d plugin -bios $<

clean:
	rm -rf $(OUT_DIR) $(RESULTS_DIR)

keep-preprocessed: $(PREPROCESSED)

.PHONY: keep-preprocessed