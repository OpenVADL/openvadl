#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETEXPANDPSEUDO_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETEXPANDPSEUDO_H

#include "[(${namespace})].h"
#include "[(${namespace})]InstrInfo.h"
#include "[(${namespace})]TargetMachine.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "Utils/ImmediateUtils.h"

#include "llvm/CodeGen/LivePhysRegs.h"
#include "llvm/CodeGen/MachineFunctionPass.h"
#include "llvm/CodeGen/MachineInstrBuilder.h"

using namespace llvm;

#ifndef MACHINE_BASIC_BLOCK_ITERATOR_TYPEDEF
#define MACHINE_BASIC_BLOCK_ITERATOR_TYPEDEF
// this typedef is a workaround for the generation tool as there are difficulties to emit types with "::"
typedef MachineBasicBlock::iterator MachineBasicBlockIterator;
#endif // MACHINE_BASIC_BLOCK_ITERATOR_TYPEDEF

#define DEBUG_TYPE "[(${namespace})]-expand-pseudo"
#define [(${namespace})]_EXPAND_PSEUDO_NAME "[(${namespace})] pseudo instruction expansion pass"

namespace llvm
{
    class [(${namespace})]ExpandPseudo : public MachineFunctionPass
    {
    public:
        const [(${namespace})]InstrInfo *TII;
        static char ID;

        explicit [(${namespace})]ExpandPseudo() : MachineFunctionPass(ID) {}

        bool runOnMachineFunction(MachineFunction & MF) override;

        StringRef getPassName() const override { return [(${namespace})]_EXPAND_PSEUDO_NAME; }

    private:
        bool expandMBB(MachineBasicBlock & MBB);
        bool expandMI(MachineBasicBlock & MBB, MachineBasicBlock::iterator MBBI, MachineBasicBlock::iterator & NextMBBI);

        bool isExpandable(MachineInstr & MI);
        bool requiresExpansion(MachineInstr & MI);

        MachineOperand copyImmOp(const MachineOperand &MO, unsigned TargetFlag = [(${namespace})]BaseInfo::MO_None, ImmediateUtils::[(${namespace})]ImmediateKind ImmediateFlag = ImmediateUtils::IK_UNKNOWN_IMMEDIATE);

        MachineOperand copyRegOp(const MachineOperand &MO, bool isDef = false, unsigned SubReg = 0);

        // auto generated
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]TARGETEXPANDPSEUDO_H