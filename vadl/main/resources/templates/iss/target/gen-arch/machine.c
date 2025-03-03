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
      [# th:each="reg_file, iterState : ${register_files}"] // CPU register file(s)
        VMSTATE_UINTTL_ARRAY(env.[(${reg_file.name_lower})], [(${gen_arch_upper})]CPU, [(${reg_file["size"]})]),
      [/]
      [# th:each="reg, iterState : ${registers}"] // CPU registers
        VMSTATE_UINTTL(env.[(${reg.name_lower})], [(${gen_arch_upper})]CPU),
      [/]

        VMSTATE_END_OF_LIST()
    }
};