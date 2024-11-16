
#include "virt.h"
#include "boot.h"
#include "qapi/error.h"
#include "qemu/qemu-print.h"
#include "exec/address-spaces.h"
#include "sysemu/sysemu.h"
#include "qemu/error-report.h"
#include "hw/char/riscv_htif.h"
#include <stdlib.h>

static const MemMapEntry virt_memmap[] = {
  [VIRT_DRAM] =         { 0x80000000,           0x0 },
  [VIRT_DRAM] = {0x80000000, 0x0},
};

static bool tofromhost_defined = false;

/*
 * Checks if a fromhost/tohost symbol was found. This is used to define if we use
 * our custom fromhost/tohost addresses or the ones defined in the elf.
 */
static void virt_sym_cb(const char *st_name, int st_info, uint64_t st_value,
                        uint64_t st_size) {
    if (strcmp("fromhost", st_name) == 0 || strcmp("tohost", st_name) == 0) {
        tofromhost_defined = true;
    }
    htif_symbol_callback(st_name, st_info, st_value, st_size);
}

static void virt_machine_ready(Notifier *notifier, void *data)
{
    // load the firmware
    qemu_printf("[VADL] virt_machine_ready\n");

    [(${gen_arch_upper})]VirtMachineState *s = container_of(notifier, [(${gen_arch_upper})]VirtMachineState, machine_ready);
    const MemMapEntry *memmap = virt_memmap;
    MachineState *machine = MACHINE(s);
    target_ulong start_addr = memmap[VIRT_DRAM].base;
    target_ulong firmware_end_addr;
    MemoryRegion *system_memory = get_system_memory();

    // currently the -bios flag must be set
    // as no default firmware is provided
    if (!machine->firmware || strcmp(machine->firmware, "none") == 0) {
        error_report("Machine requires -bios, as currently no default firmware is provided.");
        exit(1);
    }

    firmware_end_addr = [(${gen_arch_lower})]_find_and_load_firmware(machine, start_addr, virt_sym_cb);

    if (firmware_end_addr == start_addr) {
        error_report("Failed to load firmware.");
        exit(1);
    }

    qemu_printf("[VADL] firmware loaded from %x to %x\n", start_addr, firmware_end_addr);


    // now we can init the htif, as it requires the firmware callbacks to be loaded
    htif_mm_init(system_memory, serial_hd(0), memmap[VIRT_HTIF].base, !tofromhost_defined);
}

static void virt_machine_init(MachineState *machine)
{
    qemu_printf("[VADL] virt_machine_init\n");
    const MemMapEntry *memmap = virt_memmap;
    [(${gen_arch_upper})]VirtMachineState *s = [(${gen_arch_upper})]_VIRT_MACHINE(machine);

    qemu_printf("[VADL] ram-size: %x\n", machine->ram_size);
    MemoryRegion *system_memory = get_system_memory();
    qemu_printf("[VADL] sys mem size: %x\n", system_memory->size);


    object_initialize_child(OBJECT(machine), "cpu", &s->cpu, TYPE_[(${gen_arch_upper})]_CPU);
    qdev_realize(DEVICE(&s->cpu), NULL, &error_fatal);


    // add the ram region
    memory_region_add_subregion(system_memory, memmap[VIRT_DRAM].base, machine->ram);

    s->machine_ready.notify = virt_machine_ready;
    qemu_add_machine_init_done_notifier(&s->machine_ready);
}

	static void virt_machine_class_init(ObjectClass *oc, void *data) {
    MachineClass *mc = MACHINE_CLASS(oc);

    mc->desc = "[(${gen_arch})] VirtIO board";

    mc->init = virt_machine_init;
    mc->default_cpus = 1;
    mc->default_cpu_type = TYPE_VADL_CPU,
    mc->min_cpus = mc->default_cpus;
    mc->max_cpus = mc->default_cpus;
    mc->no_floppy = 1;
    mc->no_cdrom = 1;
    mc->no_parallel = 1;
    // required to add ram memory region
    mc->default_ram_id = "ram";
}

static void virt_machine_instance_init(Object *obj) {
    qemu_printf("[VADL] virt_machine_instance_init\n");
    VADLVirtMachineState *m_state = VADL_VIRT_MACHINE(obj);

    // nothing to do
}

static const TypeInfo virt_machine_typeinfo = {
    .name = TYPE_[(${gen_arch_upper})]_VIRT_MACHINE,
    .parent = TYPE_MACHINE,
    .class_init = virt_machine_class_init,
    .instance_init = virt_machine_instance_init,
    .instance_size = sizeof([(${gen_arch_upper})]VirtMachineState),
};

static void virt_machine_init_register_types(void)
{
    type_register_static(&virt_machine_typeinfo);
}

type_init(virt_machine_init_register_types)