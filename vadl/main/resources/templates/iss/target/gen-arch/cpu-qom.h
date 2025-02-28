#ifndef [(${gen_arch_upper})]_CPU_QOM_H
#define [(${gen_arch_upper})]_CPU_QOM_H

#include "hw/core/cpu.h"
#include "qom/object.h"

#define TYPE_[(${gen_arch_upper})]_CPU "[(${gen_arch})]-cpu"

OBJECT_DECLARE_CPU_TYPE([(${gen_arch_upper})]CPU, [(${gen_arch_upper})]CPUClass, [(${gen_arch_upper})]_CPU)


#endif