#include "qemu/osdep.h"
#include "exec/cpu-defs.h"
#include "hw/boards.h"
#include "boot.h"
#include "qemu/datadir.h"
#include "qemu/error-report.h"
#include "hw/loader.h"
#include "elf.h"
#include "qemu/qemu-print.h"


static char *[(${gen_arch_lower})]_find_bios(const char *bios_filename)
{
    char *filename = NULL;

    filename = qemu_find_file(QEMU_FILE_TYPE_BIOS, bios_filename);
    if (filename == NULL) {
        error_report("Unable to find the [(${gen_arch})] BIOS '%s'", bios_filename);
        exit(1);
    }

    return filename;
}


target_ulong [(${gen_arch_lower})]_find_and_load_firmware(MachineState *machine,
                                         hwaddr firmware_load_addr,
                                         symbol_fn_t symbol_fn)
{
    g_autofree char *firmware_filename = NULL;
    target_ulong firmware_end_addr = firmware_load_addr;

    firmware_filename = [(${gen_arch_lower})]_find_bios(machine->firmware);

    if (firmware_filename) {
        firmware_end_addr = [(${gen_arch_lower})]_load_firmware(firmware_filename, firmware_load_addr, symbol_fn);
    }

    return firmware_end_addr;
}

target_ulong [(${gen_arch_lower})]_load_firmware(const char *firmware_filename,
                                hwaddr firmware_load_addr,
                                symbol_fn_t sym_cb)
{
    uint64_t firmware_entry, firmware_end;
    ssize_t  firmware_size;
    bool     big_endian = false;
    // change this if something does not work
    int      elf_machine = EM_NONE;
//    int      elf_machine = EM_RISCV;
    bool     clear_lsb = true;
    bool     data_swap = false;

    g_assert(firmware_filename != NULL);

    if (load_elf_ram_sym(firmware_filename, NULL, NULL, NULL,
                         &firmware_entry, NULL, &firmware_end, NULL,
                         big_endian, elf_machine, clear_lsb, data_swap,
                         NULL, true, sym_cb) > 0) {
        return firmware_end;
    }

    qemu_printf("[VADL] failed to load_elf_ram, will try to load_image_targphys_as\n");

    firmware_size = load_image_targphys_as(firmware_filename,
                                           firmware_load_addr,
                                           current_machine->ram_size, NULL);

    if (firmware_size > 0) {
        return firmware_load_addr + firmware_size;
    }

    error_report("could not load firmware '%s'", firmware_filename);
    exit(1);
}