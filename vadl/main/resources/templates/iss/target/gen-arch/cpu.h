// VADL generated file
#ifndef QEMU_[(${gen_arch_upper})]_CPU_H
#define QEMU_[(${gen_arch_upper})]_CPU_H

#include "cpu-qom.h"
#include "exec/cpu-defs.h"
#include "qemu/typedefs.h"

#define CPU_RESOLVING_TYPE TYPE_[(${gen_arch_upper})]_CPU

[# th:each="reg_file, iterState : ${register_files}"] // define the register file sizes
#define [[${gen_arch_upper + '_REG_FILE_' + reg_file.get('name_upper') + '_SIZE'}]] [(${reg_file.get("size")})]
[/]

// the CPU environment across all cores/ArchCPU instances.
// e.g. it holds the state of all registers.
typedef struct CPUArchState {

  [# th:each="reg_file, iterState : ${register_files}"] // CPU register file(s)
  [(${reg_file.get("value_c_type")})] [(${reg_file.get("name_lower")})][[${'[' + gen_arch_upper + '_REG_FILE_' + reg_file.get('name_upper') + '_SIZE' + ']'}]];
  [/]
} CPU[(${gen_arch_upper})]State;

// state of a single core. this is declare in cpu-qom.h as VADLCPU
struct ArchCPU {
  /*< private >*/
  CPUState parent_obj;

  /*< public >*/
  CPU[(${gen_arch_upper})]State env;
};


static inline void cpu_get_tb_cpu_state(CPU[(${gen_arch_upper})]State *env, vaddr *pc,
                                        uint64_t *cs_base, uint32_t *pflags)
{
    // TODO: Implement !
    *pc = 0;
    *cs_base = 0;
    *pflags = 0;
}

#include "exec/cpu-all.h"

#endif