#ifndef QEMU_[(${gen_arch_upper})]_CPU_H
#define QEMU_[(${gen_arch_upper})]_CPU_H

#include "cpu-qom.h"
#include "exec/cpu-defs.h"
#include "qemu/typedefs.h"
#include "qemu/cpu-float.h"
#include "cpu-bits.h"

#define CPU_RESOLVING_TYPE TYPE_[(${gen_arch_upper})]_CPU

// no default memory ordering
#define TCG_GUEST_DEFAULT_MO 0

#define [(${gen_arch_upper})]_PC [(${pc_info.accessor})]
#define [(${gen_arch_upper})]_PC_TYPE [(${pc_info.value_c_type})]

#define MMU_USER_IDX 0

[# th:each="reg : ${register_tensors}"][# th:if="${reg.index_dims.size} > 0"]
extern const char * const [(${gen_arch_lower})]_cpu_[(${reg.name_lower})]_names[(${reg.c_reg_name_array_def})];
[/][/]

// the CPU environment across all cores/ArchCPU instances.
// e.g. it holds the state of all registers.
typedef struct CPUArchState {
  // CPU registers
  [# th:each="reg, iterState : ${register_tensors}"]
  uint[(${reg.cpu_state_type_width})]_t [(${reg.name_lower})][(${reg.c_array_def})][(${reg.cpu_state_alignment})];
  [/]

  // Exception arguments (intermediate store during exception handling)
  [# th:each="exc : ${exc_info.exceptions}"] [# th:each="p : ${exc.params}"]
  [(${p.c_type})] [(${p.name_in_cpu})];
  [/][/]

  [# th:if="${float_facts.has_float_ops}"]
  float_status fp_status; // float status and stick fe flags
  [# th:if="${float_facts.has_non_sticky_flags}"]
  uint16_t ns_fe_flags;   // non-sticky fe flags
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

  uint32_t flags = 0;
  uint64_t off = 0;
[# th:each="reg, iterState : ${register_tensors}"][# th:if="${reg.is_tb_state}"][# th:each="part : ${reg.tb_state_parts}"]
  flags |= ((env->[(${reg.name_lower})] >> [(${part.lsb})]) & [(${part.mask})]) << off;
  off += [(${part.width})];[/][/][/]
  *pflags = flags;
}

void [(${gen_arch_lower})]_tcg_init(void);

int [(${gen_arch_lower})]_cpu_gdb_read_register(CPUState *cpu, GByteArray *buf, int reg);
int [(${gen_arch_lower})]_cpu_gdb_write_register(CPUState *cpu, uint8_t *buf, int reg);

// CPU register getters and setters
[# th:each="access : ${base_accessors}"]
[(${access.signature})];
[/]
[# th:each="access : ${base_clear_cpu_accessors}"]
[(${access.signature})];
[/]

// Alias register accessors consumed by unified ISS helper/procedure/exception paths.
[# th:each="access : ${alias_cpu_read_accessors}"]
[(${access.signature})];
[/]
[# th:each="access : ${alias_cpu_write_accessors}"]
[(${access.signature})];
[/]
[# th:each="access : ${base_chunk_cpu_read_accessors}"]
[(${access.signature})];
[/]
[# th:each="access : ${base_chunk_cpu_write_accessors}"]
[(${access.signature})];
[/]


#include "exec/cpu-all.h"

#endif
