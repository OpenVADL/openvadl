#include "qemu/osdep.h"
#include "qapi/error.h"
#include "qemu/qemu-print.h"
#include "exec/exec-all.h"
#include "cpu.h"
#include "cpu-bits.h"
#include "disas/dis-asm.h"
#include "trace.h"
#include "tcg/debug-assert.h"
#include "hw/qdev-properties.h"
#include "vadl-builtins.h"

static [(${gen_arch_upper})]CPU* cpu_self;

[# th:each="reg : ${register_tensors}"][# th:if="${reg.index_dims.size} > 0"]
const char * const [(${gen_arch_lower})]_cpu_[(${reg.name_lower})]_names[(${reg.c_array_def})] = {
  "[(${#strings.arrayJoin(reg.names, '", "')})]"
};
[/][/]

[# th:each="reg : ${register_tensors}"]
static [(${reg.value_c_type})] get_[(${reg.name_lower})]([(${reg.getter_params_no_comma})])
{   [# th:each="dim : ${reg.index_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    [# th:each="constraint : ${reg.constraints}"]
    if ([(${constraint.check})]) return [(${constraint.value})];
    [/]
    return cpu_self->env.[(${reg.name_lower})][# th:each="dim : ${reg.index_dims}"][ [(${dim.arg_name})]][/];
}

static void set_[(${reg.name_lower})]([(${reg.getter_params_post_comma})] [(${reg.value_c_type})] val)
{   [# th:each="dim : ${reg.index_dims}"]
    assert( [(${dim.arg_name})] < [(${dim["size"]})]); [/]
    [# th:each="constraint : ${reg.constraints}"]
    if ([(${constraint.check})]) return;
    [/]
    cpu_self->env.[(${reg.name_lower})][# th:each="dim : ${reg.index_dims}"][ [(${dim.arg_name})]][/] = val;
}
[/]

static void [(${gen_arch_lower})]_cpu_disas_set_info(CPUState *cpu, disassemble_info *info)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    printf("[VADL-DISAS] [(${gen_arch_lower})]_cpu_disas_set_info\n");
    info->mach = bfd_arch_[(${gen_arch_lower})];
}

static void [(${gen_arch_lower})]_cpu_init(Object *obj)
{
  // TODO: CPU initialize work
}

// Realize function that sets up the CPU
static void [(${gen_arch_lower})]_cpu_realizefn(DeviceState *dev, Error **errp)
{
    CPUState *cs = CPU(dev);
    cpu_self = [(${gen_arch_upper})]_CPU(cs);
    [(${gen_arch_upper})]CPUClass *vcc = [(${gen_arch_upper})]_CPU_GET_CLASS(dev);
    Error *local_err = NULL;

    cpu_exec_realizefn(cs, &local_err);
    if (local_err != NULL) {
        error_propagate(errp, local_err);
        return;
    }

    qemu_init_vcpu(cs);
    cpu_reset(cs);
    vcc->parent_realize(dev, errp);
}

static void [(${gen_arch_lower})]_cpu_reset_hold(Object *obj, ResetType type)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    CPUState *cs = CPU(obj);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);
    [(${gen_arch_upper})]CPUClass *vcc = [(${gen_arch_upper})]_CPU_GET_CLASS(obj);
    CPU[(${gen_arch_upper})]State *env = &cpu->env;
    int i;

    if (vcc->parent_phases.hold) {
        vcc->parent_phases.hold(obj, type);
    }

    [# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.index_dims.size} == 0"]
    env->[(${reg.name_lower})] = 0; [/][/]
    [# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.index_dims.size} > 0"]
    memset(env->[(${reg.name_lower})], 0, sizeof(env->[(${reg.name_lower})])); [/][/]

[(${reset})]
}

static ObjectClass* [(${gen_arch_lower})]_cpu_class_by_name(const char *cpu_model)
{
    return object_class_by_name(cpu_model);
}

static bool [(${gen_arch_lower})]_cpu_has_work(CPUState *cs)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    // TODO: Could be true (RISCV)
    return true;
}

static int [(${gen_arch_lower})]_cpu_mmu_index(CPUState *cs, bool ifetch) {
    // TODO: What should we do here?
    return 0;
}

//When using the '-d cpu' paramater, QEMU executes this function after every translation block
//to give us the CPU state at the begining of the block.
static void [(${gen_arch_lower})]_cpu_dump_state(CPUState *cs, FILE *f, int flags)
{
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);
    //The CPU environment is used to access the content of the emulated registers.
    CPU[(${gen_arch_upper})]State *env = &cpu->env;

[(${reg_dump_code})]
    qemu_fprintf(f, "\n");
}

static void [(${gen_arch_lower})]_cpu_set_pc(CPUState *cs, vaddr value)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);

    cpu->env.[(${gen_arch_upper})]_PC = value;
}

// include exception handling procedures
#include "do_exception.c.inc"

static void [(${gen_arch_lower})]_cpu_do_interrupt(CPUState *cs)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);

    [(${gen_arch_upper})]CPU *cpu      = [(${gen_arch_upper})]_CPU(cs);
    CPU[(${gen_arch_upper})]State *env = &cpu->env;

    switch (cs->exception_index) {
       [# th:each="exc : ${exc_info.exceptions}"]
       case [(${exc.enum_name})]: [(${exc.handling_func})](env); break;
       [/]
    }

    // mark handled
    cs->exception_index = [(${gen_arch_upper})]_EXCP_NONE;
}

static hwaddr [(${gen_arch_lower})]_cpu_get_phys_page_debug(CPUState *cs, vaddr addr)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    return addr; /* I assume 1:1 address correspondence */
}

static int [(${gen_arch_lower})]_cpu_memory_rw_debug(CPUState *cs, vaddr addr, uint8_t *buf, int len, bool is_write)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    // TODO: Later
    return -1;
}

static bool [(${gen_arch_lower})]_cpu_exec_interrupt(CPUState *cs, int interrupt_request)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    // should never happen right now (no irq handling)
    assert(false);
    // TODO: Later
    return false;
}

static bool [(${gen_arch_lower})]_cpu_exec_halt(CPUState *cs)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    // TODO: Handle stuff that must be done when CPU is in halted state
    return [(${gen_arch_lower})]_cpu_has_work(cs);

}

static void [(${gen_arch_lower})]_cpu_restore_state_to_opc(CPUState *cs, const TranslationBlock *tb, const uint64_t *data) {
    trace_[(${gen_arch_lower})]_cpu_call(__func__);

    [(${gen_arch_upper})]CPU *cpu      = [(${gen_arch_upper})]_CPU(cs);
    CPU[(${gen_arch_upper})]State *env = &cpu->env;

    env->[(${gen_arch_upper})]_PC = data[0];
}

static bool [(${gen_arch_lower})]_cpu_tlb_fill(CPUState *cs, vaddr address, int size,
                       MMUAccessType access_type, int mmu_idx,
                       bool probe, uintptr_t retaddr)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);

    // TODO: Make eventually better
    int port = 0;
    port = PAGE_READ | PAGE_EXEC | PAGE_WRITE;
    tlb_set_page(cs, address, address, port, mmu_idx, TARGET_PAGE_SIZE);
    return true;
}

static void [(${gen_arch_lower})]_cpu_synchronize_from_tb(CPUState *cs, const TranslationBlock *tb)
{
    trace_[(${gen_arch_lower})]_cpu_call(__func__);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);
    cpu->env.[(${gen_arch_upper})]_PC = tb->pc;
}


#include "hw/core/sysemu-cpu-ops.h"

static const struct SysemuCPUOps [(${gen_arch_lower})]_sysemu_ops = {
    .get_phys_page_debug = [(${gen_arch_lower})]_cpu_get_phys_page_debug,
};

#include "hw/core/tcg-cpu-ops.h"

// Here we map the above function to their pupose,
// so that QEMU knows when to execute them.
static const struct TCGCPUOps [(${gen_arch_lower})]_tcg_ops = {
    .initialize = [(${gen_arch_lower})]_tcg_init,
    .synchronize_from_tb = [(${gen_arch_lower})]_cpu_synchronize_from_tb,
    .cpu_exec_interrupt = [(${gen_arch_lower})]_cpu_exec_interrupt,
    .cpu_exec_halt = [(${gen_arch_lower})]_cpu_exec_halt,
    .tlb_fill = [(${gen_arch_lower})]_cpu_tlb_fill,
    .do_interrupt = [(${gen_arch_lower})]_cpu_do_interrupt,
    .restore_state_to_opc = [(${gen_arch_lower})]_cpu_restore_state_to_opc,
};

static Property [(${gen_arch_lower})]_cpu_properties[] = {
    DEFINE_PROP_END_OF_LIST(),
};

static void [(${gen_arch_lower})]_cpu_class_init(ObjectClass *oc, void *data)
{
    [(${gen_arch_upper})]CPUClass *vcc = [(${gen_arch_upper})]_CPU_CLASS(oc);
    CPUClass *cc = CPU_CLASS(oc);
    DeviceClass *dc = DEVICE_CLASS(oc);
    ResettableClass *rc = RESETTABLE_CLASS(oc);

    device_class_set_parent_realize(dc, [(${gen_arch_lower})]_cpu_realizefn, &vcc->parent_realize);
    resettable_class_set_parent_phases(rc, NULL, [(${gen_arch_lower})]_cpu_reset_hold, NULL, &vcc->parent_phases);

    cc->class_by_name = [(${gen_arch_lower})]_cpu_class_by_name;

    cc->has_work = [(${gen_arch_lower})]_cpu_has_work;
    cc->mmu_index = [(${gen_arch_lower})]_cpu_mmu_index;
    cc->dump_state = [(${gen_arch_lower})]_cpu_dump_state;
    cc->set_pc = [(${gen_arch_lower})]_cpu_set_pc;
    cc->memory_rw_debug = [(${gen_arch_lower})]_cpu_memory_rw_debug;
    cc->sysemu_ops = &[(${gen_arch_lower})]_sysemu_ops;
    cc->disas_set_info = [(${gen_arch_lower})]_cpu_disas_set_info;
    cc->tcg_ops = &[(${gen_arch_lower})]_tcg_ops;

    // GDB settings
    cc->gdb_read_register = [(${gen_arch_lower})]_cpu_gdb_read_register;
    cc->gdb_write_register = [(${gen_arch_lower})]_cpu_gdb_write_register;
    cc->gdb_core_xml_file = "[(${gen_arch_lower})]-cpu.xml";
    cc->gdb_stop_before_watchpoint = true;

    // CPU Properties
    device_class_set_props(dc, [(${gen_arch_lower})]_cpu_properties);
}


static const TypeInfo [(${gen_arch_lower})]_cpu_arch_types[] = {
    {
        .name = TYPE_[(${gen_arch_upper})]_CPU,
        .parent = TYPE_CPU,
        .instance_size = sizeof([(${gen_arch_upper})]CPU),
        .instance_init = [(${gen_arch_lower})]_cpu_init,
        .class_size = sizeof([(${gen_arch_upper})]CPUClass),
        .class_init = [(${gen_arch_lower})]_cpu_class_init,
    }
};

static void [(${gen_arch_lower})]_cpu_register_types(void)
{
    int i;

    type_register_static_array([(${gen_arch_lower})]_cpu_arch_types, 1);
}

type_init([(${gen_arch_lower})]_cpu_register_types)