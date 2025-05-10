#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]DAGTODDAGISEL_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]DAGTODDAGISEL_H

#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]SubTarget.h"
#include "[(${namespace})]TargetMachine.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "Utils/ImmediateUtils.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/CodeGen/SelectionDAGISel.h"
#include "llvm/Target/TargetMachine.h"

namespace llvm
{
    class [(${namespace})]DAGToDAGISelLegacy : public SelectionDAGISelLegacy
    {
    public:
            static char ID;
            explicit [(${namespace})]DAGToDAGISelLegacy( [(${namespace})]TargetMachine & TargetMachine, CodeGenOptLevel OptLevel);
    };

    class [(${namespace})]DAGToDAGISel : public SelectionDAGISel
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();
        const [(${namespace})]Subtarget *Subtarget = nullptr;

    public:
        explicit [(${namespace})]DAGToDAGISel( [(${namespace})]TargetMachine & TargetMachine, CodeGenOptLevel OptLevel) : SelectionDAGISel(TargetMachine, OptLevel) {}

        bool runOnMachineFunction(MachineFunction & MF) override
        {
            Subtarget = &MF.getSubtarget<[(${namespace})]Subtarget>();
            return SelectionDAGISel::runOnMachineFunction(MF);
        }

        bool SelectInlineAsmMemoryOperand(const SDValue &Op, InlineAsm::ConstraintCode ConstraintCode, std::vector<SDValue> &OutOps) override;
        void Select(SDNode * N) override;
        bool SelectAddrFI(SDValue Addr, SDValue & Base);

    private:
        bool trySelect(SDNode * Node);

// needs static methods from [(${namespace})]BaseInfo.h
#include "[(${namespace})]GenDAGISel.inc"
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]DAGTODDAGISEL_H