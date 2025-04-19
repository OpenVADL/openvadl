#ifndef QEMU_[(${gen_arch_upper})]_CPU_H
#define QEMU_[(${gen_arch_upper})]_CPU_H

#include "cpu-qom.h"
#include "exec/cpu-defs.h"
#include "qemu/typedefs.h"
#include "cpu-bits.h"

#define CPU_RESOLVING_TYPE TYPE_[(${gen_arch_upper})]_CPU

// no default memory ordering
#define TCG_GUEST_DEFAULT_MO 0

#define [(${gen_arch_upper})]_PC [(${pc_reg.name_lower})]
#define [(${gen_arch_upper})]_PC_TYPE [(${pc_reg.value_c_type})]

[# th:each="reg : ${register_tensors}"][# th:if="${reg.index_dims.size} > 0"]
extern const char * const [(${gen_arch_lower})]_cpu_[(${reg.name_lower})]_names[(${reg.c_array_def})];
[/][/]

// the CPU environment across all cores/ArchCPU instances.
// e.g. it holds the state of all registers.
typedef struct CPUArchState {
  // CPU registers
  [# th:each="reg, iterState : ${register_tensors}"]
  [(${reg.value_c_type})] [(${reg.name_lower})][(${reg.c_array_def})];
  [/]

  // pc reset vector
  uint64_t reset_vec;

  // Exception arguments (intermediate store during exception handling)
  [# th:each="exc : ${exc_info.exceptions}"] [# th:each="p : ${exc.params}"]
  [(${p.c_type})] [(${p.name_in_cpu})];
  [/][/]
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
  ResettablePhases parent_phases;
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

int [(${gen_arch_lower})]_cpu_gdb_read_register(CPUState *cpu, GByteArray *buf, int reg);
int [(${gen_arch_lower})]_cpu_gdb_write_register(CPUState *cpu, uint8_t *buf, int reg);


#include "exec/cpu-all.h"

#endif
