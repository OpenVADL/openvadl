
#ifndef HW_[(${gen_arch_upper})]_[(${gen_machine_upper})]_H
#define HW_[(${gen_arch_upper})]_[(${gen_machine_upper})]_H

#include "qemu/osdep.h"
#include "qemu/typedefs.h"
#include "qom/object.h"
#include "hw/boards.h"
#include "cpu-qom.h"
#include "cpu.h"

// TODO: Make it dynamic
#define [(${gen_arch_upper})]_[(${gen_machine_upper})]_FLASH_SIZE 1024 * KiB

#define TYPE_[(${gen_arch_upper})]_[(${gen_machine_upper})]_MACHINE MACHINE_TYPE_NAME("[(${gen_machine_lower})]")

/* No class required as it is just a state object without methods */
OBJECT_DECLARE_SIMPLE_TYPE([(${gen_arch_upper})][(${gen_machine})]MachineState, [(${gen_arch_upper})]_[(${gen_machine_upper})]_MACHINE)

struct [(${gen_arch_upper})][(${gen_machine})]MachineState {
  /*< private >*/
  MachineState parent;

  /*< public >*/
  Notifier machine_ready;
  [(${gen_arch_upper})]CPU cpu;
};

enum {
  [# th:if="${mem_info.rom_size} != 0"][(${gen_machine_upper})]_MROM,[/]
  // location where bios is load to
  [(${gen_machine_upper})]_DRAM
};

#endif