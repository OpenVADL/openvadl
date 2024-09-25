
#include "qemu/osdep.h"
#include "qapi/error.h"
#include "qemu/qemu-print.h"
#include "exec/exec-all.h"
#include "cpu.h"
#include "disas/dis-asm.h"
#include "tcg/debug-assert.h"

static [(${gen_arch_upper})]CPU* cpu_self;

[# th:each="reg_file, iterState : ${register_files}"] // define the register file sizes
static const char * const [(${gen_arch_lower})]_cpu_[(${reg_file.name_lower})]_names[[(${"[" + reg_file["size"] + "]"})]] = {
  "[(${#strings.arrayJoin(reg_file.names, '", "')})]"
};
[/]

static void [(${gen_arch_lower})]_cpu_disas_set_info(CPUState *cpu, disassemble_info *info)
{
    printf("[VADL-DISAS] [(${gen_arch_lower})]_cpu_disas_set_info\n");
    info->mach = bfd_arch_[(${gen_arch_lower})];
}

static void [(${gen_arch_lower})]_cpu_init(Object *obj)
{
  qemu_printf("[VADL] cpu_init\n");

  CPUState *cs = CPU(obj);
  [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(obj);
  CPU[(${gen_arch_upper})]State *env = &cpu->env;
}

// Realize function that sets up the CPU
static void [(${gen_arch_lower})]_cpu_realizefn(DeviceState *dev, Error **errp)
{
	  qemu_printf("[VADL] cpu_realizefn\n");
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

static void [(${gen_arch_lower})]_cpu_reset(DeviceState *dev)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_reset\n");
    CPUState *cs = CPU(dev);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(dev);
    [(${gen_arch_upper})]CPUClass *vcc = [(${gen_arch_upper})]_CPU_GET_CLASS(dev);
    CPU[(${gen_arch_upper})]State *env = &cpu->env;
    int i;

    vcc->parent_reset(dev);

    [# th:each="reg_file, iterState : ${register_files}"]
    for(int i=0; i < [(${reg_file.size})]; i++){
      env->[(${reg_file.name_lower})][i] = 0;
    }
    [/]

    // Start address of the execution. Notice, that this is the start of the flash memory address
    // from the virt board implementation.
    env->[(${gen_arch_upper})]_PC = 0x80000000;
}

static ObjectClass* [(${gen_arch_lower})]_cpu_class_by_name(const char *cpu_model)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_class_by_name\n");
    return object_class_by_name(cpu_model);
}

static bool [(${gen_arch_lower})]_cpu_has_work(CPUState *cs)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_has_work\n");
    // TODO: Could be true (RISCV)
    return true;
}

static int [(${gen_arch_lower})]_cpu_mmu_index(CPUState *cs, bool ifetch) {
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_mmu_index\n");
    // TODO: What should we do here?
    return 0;
}

//When using the '-d cpu' paramater, QEMU executes this function after every translation block
//to give us the CPU state at the begining of the block.
static void [(${gen_arch_lower})]_cpu_dump_state(CPUState *cs, FILE *f, int flags)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_dump_state\n");
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);
    //The CPU environment is used to access the content of the emulated registers.
    CPU[(${gen_arch_upper})]State *env = &cpu->env;
    [# th:each="reg, iterState : ${registers}"]
    qemu_fprintf(f, " [(${reg.name})]:    " TARGET_FMT_lx "\n", env->[(${reg.name_lower})]);
    [/]
    int i;
    [# th:each="reg_file, iterState : ${register_files}"]
    for(i=0; i < [(${reg_file.size})]; i++){
      qemu_fprintf(f, " %-8s " TARGET_FMT_lx, [(${gen_arch_lower})]_cpu_[(${reg_file.name_lower})]_names[i], env->[(${reg_file.name_lower})][i]);
      if ((i & 3) == 3) {
          qemu_fprintf(f, "\n");
      }
    }
    [/]
    qemu_fprintf(f, "\n");
}

static void [(${gen_arch_lower})]_cpu_set_pc(CPUState *cs, vaddr value)
{
    qemu_printf("[VADL-CPU] [(${gen_arch_lower})]_cpu_set_pc, pc: %04lx\n", value);
    [(${gen_arch_upper})]CPU *cpu = [(${gen_arch_upper})]_CPU(cs);

    cpu->env.[(${gen_arch_upper})]_PC = value;
}

static void [(${gen_arch_lower})]_cpu_do_interrupt(CPUState *cpu)
{
    qemu_printf("[VADL] TODO: [(${gen_arch_lower})]_do_interrupt\n");
}

static hwaddr [(${gen_arch_lower})]_cpu_get_phys_page_debug(CPUState *cs, vaddr addr)
{
    qemu_printf("[VADL] TODO: [(${gen_arch_lower})]_cpu_get_phys_page_debug\n");
    return addr; /* I assume 1:1 address correspondence */
}

static int [(${gen_arch_lower})]_cpu_memory_rw_debug(CPUState *cs, vaddr addr, uint8_t *buf, int len, bool is_write)
{
    qemu_printf("[VADL] TODO: [(${gen_arch_lower})]_cpu_memory_rw_debug\n");
    // TODO: Later
    return -1;
}

static bool [(${gen_arch_lower})]_cpu_exec_interrupt(CPUState *cs, int interrupt_request)
{
    qemu_printf("[VADL] TODO: [(${gen_arch_lower})]_cpu_exec_interrupt\n");
    // TODO: Later
    return false;
}

static void [(${gen_arch_lower})]_cpu_exec_halt(CPUState *cs)
{
    qemu_printf("[VADL] TODO: [(${gen_arch_lower})]_cpu_exec_halt\n");
    // TODO: Later
}

static bool [(${gen_arch_lower})]_cpu_tlb_fill(CPUState *cs, vaddr address, int size,
                       MMUAccessType access_type, int mmu_idx,
                       bool probe, uintptr_t retaddr)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_tlb\n");

    // TODO: Make eventually better
    int port = 0;
    port = PAGE_READ | PAGE_EXEC | PAGE_WRITE;
    tlb_set_page(cs, address, address, port, mmu_idx, TARGET_PAGE_SIZE);
    return true;
}

static void [(${gen_arch_lower})]_cpu_synchronize_from_tb(CPUState *cs, const TranslationBlock *tb)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_synchronize_from_tb\n");
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
};

static void [(${gen_arch_lower})]_cpu_class_init(ObjectClass *oc, void *data)
{
    qemu_printf("[VADL] [(${gen_arch_lower})]_cpu_class_init\n");
    [(${gen_arch_upper})]CPUClass *vcc = [(${gen_arch_upper})]_CPU_CLASS(oc);
    CPUClass *cc = CPU_CLASS(oc);
    DeviceClass *dc = DEVICE_CLASS(oc);

    device_class_set_parent_realize(dc, [(${gen_arch_lower})]_cpu_realizefn, &vcc->parent_realize);
    device_class_set_parent_reset(dc, [(${gen_arch_lower})]_cpu_reset, &vcc->parent_reset);

    cc->class_by_name = [(${gen_arch_lower})]_cpu_class_by_name;

    cc->has_work = [(${gen_arch_lower})]_cpu_has_work;
    cc->mmu_index = [(${gen_arch_lower})]_cpu_mmu_index;
    cc->dump_state = [(${gen_arch_lower})]_cpu_dump_state;
    cc->set_pc = [(${gen_arch_lower})]_cpu_set_pc;
    cc->memory_rw_debug = [(${gen_arch_lower})]_cpu_memory_rw_debug;
    cc->sysemu_ops = &[(${gen_arch_lower})]_sysemu_ops;
    cc->disas_set_info = [(${gen_arch_lower})]_cpu_disas_set_info;
    cc->tcg_ops = &[(${gen_arch_lower})]_tcg_ops;
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