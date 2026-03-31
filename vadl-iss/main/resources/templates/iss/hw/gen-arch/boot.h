#ifndef HW_[(${gen_arch_upper})]_BOOT_H
#define HW_[(${gen_arch_upper})]_BOOT_H

#include "hw/boards.h"
#include "hw/loader.h"


target_ulong [(${gen_arch_lower})]_find_and_load_firmware(MachineState *ms,
                                                          hwaddr firmware_load_addr,
                                                          symbol_fn_t symbol_fn);

target_ulong [(${gen_arch_lower})]_load_firmware(const char *firmware_filename,
                                                 hwaddr firmware_load_addr,
                                                 symbol_fn_t symbol_fn);

#endif // HW_[(${gen_arch_upper})]_BOOT_H