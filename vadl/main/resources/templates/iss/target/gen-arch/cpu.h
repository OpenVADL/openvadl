// VADL generated file
#ifndef QEMU_[(${gen_arch_upper})]_CPU_H
#define QEMU_[(${gen_arch_upper})]_CPU_H

#include "cpu-qom.h"
#include "exec/cpu-defs.h"
#include "qemu/typedefs.h"

#define CPU_RESOLVING_TYPE TYPE_[(${gen_arch_upper})]_CPU

#define [(${gen_arch_upper})]_PC [(${pc_reg_name})]
#define [(${gen_arch_upper})]_PC_TYPE [(${pc_reg_c_type})]

[# th:each="reg_file, iterState : ${register_files}"] // define the register file sizes
#define [[${gen_arch_upper + '_REG_FILE_' + reg_file.name_upper + '_SIZE'}]] [(${reg_file["size"]})]
[/]

[# th:each="reg_file, iterState : ${register_files}"] // define the register file sizes
extern const char * const [(${gen_arch_lower})]_cpu_[(${reg_file.name_lower})]_names[(${"[" + reg_file["size"] + "]"})];
[/]

// the CPU environment across all cores/ArchCPU instances.
// e.g. it holds the state of all registers.
typedef struct CPUArchState {
  [# th:each="reg_file, iterState : ${register_files}"] // CPU register file(s)
  [(${reg_file.value_c_type})] [(${reg_file.name_lower})][[${'[' + gen_arch_upper + '_REG_FILE_' + reg_file.name_upper + '_SIZE' + ']'}]];
  [/]
  [# th:each="reg, iterState : ${registers}"] // CPU registers
  [(${reg.c_type})] [(${reg.name_lower})];
  [/]
  [# th:if="${insn_count}"]
  uint64_t insn_count;
  [/]

  // hardcoded CSR and Privilege registers
  target_ulong priv;

  uint64_t mstatus;
  target_ulong mtvec;
  target_ulong mcause;
  target_ulong mepc;
  target_ulong mtval;
} CPU[(${gen_arch_upper})]State;


// state of a single core. this is declare in cpu-qom.h as [(${gen_arch_upper})]CPU
struct ArchCPU {
  /*< private >*/
  CPUState parent_obj;

  /*< public >*/
  CPU[(${gen_arch_upper})]State env;
};

/**
 * [(${gen_arch_upper})]CPUClass:
 * @parent_realize: The parent class' realize handler.
 * @parent_phases: The parent class' reset phase handlers.
 *
 * A [(${gen_arch})] CPU model.
 */
struct [(${gen_arch_upper})]CPUClass {
  /*< private >*/
  CPUClass parent_class;

  /*< public >*/
  DeviceRealize parent_realize;
  DeviceReset   parent_reset;
};

static inline int cpu_interrupts_enabled(CPU[(${gen_arch_upper})]State *env)
{
  // TODO: CHANGE THIS
  return 0;
}

static inline void cpu_get_tb_cpu_state(CPU[(${gen_arch_upper})]State *env, vaddr *pc,
                                        uint64_t *cs_base, uint32_t *pflags)
{
    *pc = env->[(${gen_arch_upper})]_PC;
    *cs_base = 0;
    *pflags = 0;
}

void [(${gen_arch_lower})]_tcg_init(void);

#include "exec/cpu-all.h"

#endif
