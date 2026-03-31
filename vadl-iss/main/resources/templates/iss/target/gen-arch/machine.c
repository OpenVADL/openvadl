#include "qemu/osdep.h"
#include "cpu.h"
#include "qemu/error-report.h"
#include "migration/cpu.h"


// TODO: Activate/Use in cpu.h#cpu_class_init at DeviceClass#vmsd (dc->vmsd=&vms_..._cpu)
// The VMState definition that is used to save and restore
// the state of the CPU device.
const VMStateDescription vms_[(${gen_arch_lower})]_cpu = {
    .name = "cpu",
    .version_id = 1,
    .minimum_version_id = 1,
    .fields = (VMStateField[]) {
        // TODO: persist registers
        VMSTATE_END_OF_LIST()
    }
};