
#include "[(${gen_machine_lower})].h"
#include "boot.h"
#include "qapi/error.h"
#include "qemu/qemu-print.h"
#include "exec/address-spaces.h"
#include "sysemu/sysemu.h"
#include "qemu/error-report.h"
#include "hw/char/riscv_htif.h"
#include <stdlib.h>

static const MemMapEntry [(${gen_machine_lower})]_memmap[] = {
  [# th:if="${mem_info.rom_size} != 0"]
  [ [(${gen_machine_upper})]_MROM] = {[(${mem_info.rom_start})], [(${mem_info.rom_size})]},
  [/]
  [ [(${gen_machine_upper})]_DRAM] = {[(${dram_base})], 0x0},
};


[# th:if="${htif_enabled}"]
static bool tofromhost_defined = false;
[/]

static void [(${gen_machine_lower})]_sym_cb(const char *st_name, int st_info, uint64_t st_value,
                        uint64_t st_size) {
   [# th:if="${htif_enabled}"]
   // Checks if a fromhost/tohost symbol was found. If at least one was found,
   // we activate HTIF.
    if (strcmp("fromhost", st_name) == 0 || strcmp("tohost", st_name) == 0) {
        tofromhost_defined = true;
    }
    htif_symbol_callback(st_name, st_info, st_value, st_size);
    [/]
}

[# th:if="${mem_info.rom_size} != 0"]
[(${setup_rom_reset_vec})]
[/]

static void [(${gen_machine_lower})]_machine_ready(Notifier *notifier, void *data)
{
    // load the firmware

    [(${gen_arch_upper})][(${gen_machine})]MachineState *s = container_of(notifier, [(${gen_arch_upper})][(${gen_machine})]MachineState, machine_ready);
    const MemMapEntry *memmap = [(${gen_machine_lower})]_memmap;
    MachineState *machine = MACHINE(s);
    target_ulong start_addr = [(${start_addr})];
    target_ulong firmware_end_addr;
    MemoryRegion *system_memory = get_system_memory();

    // currently the -bios flag must be set
    // as no default firmware is provided
    if (!machine->firmware || strcmp(machine->firmware, "none") == 0) {
        error_report("Machine requires -bios, as currently no default firmware is provided.");
        exit(1);
    }

    firmware_end_addr = [(${gen_arch_lower})]_find_and_load_firmware(machine, start_addr, [(${gen_machine_lower})]_sym_cb);

    if (firmware_end_addr == start_addr) {
        error_report("Failed to load firmware.");
        exit(1);
    }

    [# th:if="${mem_info.rom_size} != 0"]
    setup_rom_reset_vec();
    [/]

    [# th:if="${htif_enabled}"]
    if (tofromhost_defined) {
      // now we can init the htif, as it requires the firmware callbacks to be loaded
      htif_mm_init(system_memory, serial_hd(0), 0, false);
    }[/]
}

static void [(${gen_machine_lower})]_machine_init(MachineState *machine)
{
    const MemMapEntry *memmap = [(${gen_machine_lower})]_memmap;
    [(${gen_arch_upper})][(${gen_machine})]MachineState *s = [(${gen_arch_upper})]_[(${gen_machine_upper})]_MACHINE(machine);

    MemoryRegion *system_memory = get_system_memory();


    object_initialize_child(OBJECT(machine), "cpu", &s->cpu, TYPE_[(${gen_arch_upper})]_CPU);
    qdev_realize(DEVICE(&s->cpu), NULL, &error_fatal);


    // add the ram region
    memory_region_add_subregion(system_memory, memmap[ [(${gen_machine_upper})]_DRAM].base, machine->ram);

    [# th:if="${mem_info.rom_size} != 0"]
    MemoryRegion *mask_rom = g_new(MemoryRegion, 1);
    memory_region_init_rom(mask_rom, NULL, "[(${gen_arch_lower})].[(${gen_machine_lower})].mrom",
                           memmap[ [(${gen_machine_upper})]_MROM].size, &error_fatal);
    memory_region_add_subregion(system_memory, memmap[ [(${gen_machine_upper})]_MROM].base,
                                mask_rom);
    [/]

    s->machine_ready.notify = [(${gen_machine_lower})]_machine_ready;
    qemu_add_machine_init_done_notifier(&s->machine_ready);
}

	static void [(${gen_machine_lower})]_machine_class_init(ObjectClass *oc, void *data) {
    MachineClass *mc = MACHINE_CLASS(oc);

    mc->desc = "[(${gen_arch})] [(${gen_machine})] board";

    mc->init = [(${gen_machine_lower})]_machine_init;
    mc->default_cpus = 1;
    mc->is_default = true;
    mc->default_cpu_type = TYPE_[(${gen_arch_upper})]_CPU,
    mc->min_cpus = mc->default_cpus;
    mc->max_cpus = mc->default_cpus;
    mc->no_floppy = 1;
    mc->no_cdrom = 1;
    mc->no_parallel = 1;
    // required to add ram memory region
    mc->default_ram_id = "ram";
}

static void [(${gen_machine_lower})]_machine_instance_init(Object *obj) {
    [(${gen_arch_upper})][(${gen_machine})]MachineState *m_state = [(${gen_arch_upper})]_[(${gen_machine_upper})]_MACHINE(obj);

    // nothing to do
}

static const TypeInfo [(${gen_machine_lower})]_machine_typeinfo = {
    .name = TYPE_[(${gen_arch_upper})]_[(${gen_machine_upper})]_MACHINE,
    .parent = TYPE_MACHINE,
    .class_init = [(${gen_machine_lower})]_machine_class_init,
    .instance_init = [(${gen_machine_lower})]_machine_instance_init,
    .instance_size = sizeof([(${gen_arch_upper})][(${gen_machine})]MachineState),
};

static void [(${gen_machine_lower})]_machine_init_register_types(void)
{
    type_register_static(&[(${gen_machine_lower})]_machine_typeinfo);
}

type_init([(${gen_machine_lower})]_machine_init_register_types)