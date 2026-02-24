#include "qemu/osdep.h"
#include "qapi/error.h"
#include "target/[(${gen_arch_lower})]/cpu.h"
#include "qemu/module.h"
#include "cpu.h"

static void [(${gen_arch_lower})]_cpu_realizefn(DeviceState *dev, Error **errp)
{
    CPUState *cs = CPU(dev);
    [(${gen_arch_upper})]CPUClass *mcc = [(${gen_arch_upper})]_CPU_GET_CLASS(dev);
    Error *local_err = NULL;

    cpu_exec_realizefn(cs, &local_err);
    if (local_err != NULL) {
        error_propagate(errp, local_err);
        return;
    }

    qemu_init_vcpu(cs);
    cpu_class_init_props(dev);
}

static void [(${gen_arch_lower})]_cpu_reset(DeviceState *dev)
{
    CPUState *cs = CPU(dev);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);
    [(${gen_arch_upper})]CPUClass *mcc = [(${gen_arch_upper})]_CPU_GET_CLASS(cpu);
    CPU[(${gen_arch_upper})]State *env = &cpu->env;

    mcc->parent_reset(dev);

    /* Dynamically loop through all register files defined in VADL to reset them */
    [# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.index_dims.size} == 0"]
    env->[(${reg.name_lower})] = 0; [/][/]
    
    [# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.index_dims.size} > 0"]
    memset(env->[(${reg.name_lower})], 0, sizeof(env->[(${reg.name_lower})])); [/][/]
    
    env->pc = 0;
}

static void [(${gen_arch_lower})]_cpu_set_pc(CPUState *cs, vaddr value)
{
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);
    cpu->env.pc = value;
}

static ObjectClass *[(${gen_arch_lower})]_cpu_class_by_name(const char *cpu_model)
{
    return object_class_by_name(TYPE_[(${gen_arch_upper})]_CPU);
}

static void [(${gen_arch_lower})]_cpu_class_init(ObjectClass *oc, void *data)
{
    [(${gen_arch_upper})]CPUClass *mcc = [(${gen_arch_upper})]_CPU_CLASS(oc);
    CPUClass *cc = CPU_CLASS(oc);
    DeviceClass *dc = DEVICE_CLASS(oc);

    device_class_set_parent_reset(dc, [(${gen_arch_lower})]_cpu_reset, &mcc->parent_reset);
    
    dc->realize = [(${gen_arch_lower})]_cpu_realizefn;
    
    cc->class_by_name = [(${gen_arch_lower})]_cpu_class_by_name;
    cc->has_work = NULL; 
    cc->dump_state = NULL; 
    cc->set_pc = [(${gen_arch_lower})]_cpu_set_pc;
    cc->gdb_read_register = NULL; 
    cc->gdb_write_register = NULL; 
    cc->gdb_num_core_regs = 0;
}

static const TypeInfo [(${gen_arch_lower})]_cpu_type_info = {
    .name = TYPE_[(${gen_arch_upper})]_CPU,
    .parent = TYPE_CPU,
    .instance_size = sizeof([(${gen_arch_upper})]CPU),
    .class_size = sizeof([(${gen_arch_upper})]CPUClass),
    .class_init = [(${gen_arch_lower})]_cpu_class_init,
};

static void [(${gen_arch_lower})]_cpu_register_types(void)
{
    type_register_static(&[(${gen_arch_lower})]_cpu_type_info);
}

type_init([(${gen_arch_lower})]_cpu_register_types)