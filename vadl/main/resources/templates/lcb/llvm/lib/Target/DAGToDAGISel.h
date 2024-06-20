#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]DAGTODDAGISEL_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]DAGTODDAGISEL_H

#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]Subtarget.h"
#include "[(${namespace})]TargetMachine.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "Utils/ImmediateUtils.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/CodeGen/SelectionDAGISel.h"
#include "llvm/Target/TargetMachine.h"

namespace llvm
{
    class [(${namespace})]DAGToDAGISel : public SelectionDAGISel
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();

    public:
        static char ID;
        explicit [(${namespace})]DAGToDAGISel( [(${namespace})]TargetMachine & TargetMachine, CodeGenOpt::Level OptLevel) : SelectionDAGISel(ID, TargetMachine, OptLevel) {}

        bool runOnMachineFunction(MachineFunction & MF) override
        {
            return SelectionDAGISel::runOnMachineFunction(MF);
        }

        StringRef getPassName() const override
        {
            return "[(${namespace})] DAG->DAG Pattern Instruction Selection";
        }

        bool SelectInlineAsmMemoryOperand(const SDValue &Op, unsigned ConstraintCode, std::vector<SDValue> &OutOps) override;

        void Select(SDNode * N) override;
        bool SelectAddrFI(SDValue Addr, SDValue & Base);

    private:
        bool trySelect(SDNode * Node);

// needs static methods from [(${namespace})]BaseInfo.h
#include "[(${namespace})]GenDAGISel.inc"
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]DAGTODDAGISEL_H