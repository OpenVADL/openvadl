#ifndef MYTARGET_CPU_QOM_H
#define MYTARGET_CPU_QOM_H

#include "hw/core/cpu.h"
#include "qom/object.h"

#define TYPE_MYTARGET_CPU "mytarget-cpu"

OBJECT_DECLARE_CPU_TYPE(MyTargetCPU, MyTargetCPUClass, MYTARGET_CPU)

struct MyTargetCPUClass {
    CPUClass parent_class;
    DeviceRealize parent_realize;
    void (*parent_reset)(CPUState *cpu);
};

struct MyTargetCPU {
    CPUState parent_obj;
    
    CPUArchState env;
};

#endif