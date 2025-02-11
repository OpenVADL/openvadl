
#ifndef HW_[(${gen_arch_upper})]_VIRT_H
#define HW_[(${gen_arch_upper})]_VIRT_H

#include "qemu/osdep.h"
#include "qemu/typedefs.h"
#include "qom/object.h"
#include "hw/boards.h"
#include "cpu-qom.h"
#include "cpu.h"

// TODO: Make it dynamic
#define [(${gen_arch_upper})]_VIRT_FLASH_SIZE 1024 * KiB

#define TYPE_[(${gen_arch_upper})]_VIRT_MACHINE MACHINE_TYPE_NAME("virt")

/* No class required as it is just a state object without methods */
OBJECT_DECLARE_SIMPLE_TYPE([(${gen_arch_upper})]VirtMachineState, [(${gen_arch_upper})]_VIRT_MACHINE)

struct [(${gen_arch_upper})]VirtMachineState {
  /*< private >*/
  MachineState parent;

  /*< public >*/
  Notifier machine_ready;
  [(${gen_arch_upper})]CPU cpu;
};

enum {
  // location where bios is load to
  VIRT_DRAM
};

#endif