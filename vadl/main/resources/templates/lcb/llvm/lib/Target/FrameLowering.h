#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]FRAMELOWERING_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]FRAMELOWERING_H

#include "llvm/CodeGen/TargetFrameLowering.h"

namespace llvm
{
    class [(${namespace})]Subtarget;
    class BitVector;

    class [(${namespace})]FrameLowering : public TargetFrameLowering
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();

    public:
        explicit [(${namespace})]FrameLowering(const [(${namespace})]Subtarget &STI);

        void emitPrologue(MachineFunction & MF, MachineBasicBlock & MBB) const override;
        void emitEpilogue(MachineFunction & MF, MachineBasicBlock & MBB) const override;

        bool hasFP(const MachineFunction & /*MF*/) const override;

        MachineBasicBlock::iterator eliminateCallFramePseudoInstr(MachineFunction & MF, MachineBasicBlock & MBB, MachineBasicBlock::iterator MI) const override;

        void determineCalleeSaves(MachineFunction & MF, BitVector & SavedRegs, RegScavenger *RS = nullptr) const override;

        bool spillCalleeSavedRegisters(MachineBasicBlock & MBB, MachineBasicBlock::iterator MI, ArrayRef<CalleeSavedInfo> CSI, const TargetRegisterInfo *TRI) const override;

        bool restoreCalleeSavedRegisters(MachineBasicBlock & MBB, MachineBasicBlock::iterator MI, MutableArrayRef<CalleeSavedInfo> CSI, const TargetRegisterInfo *TRI) const override;

        StackOffset getFrameIndexReference(const MachineFunction &MF, int FI, Register &FrameReg) const override;

    private:
        const [(${namespace})]Subtarget &STI;
        uint64_t getStackSize(const MachineFunction &MF) const;
        bool hasReservedCallFrame(const MachineFunction &MF) const;
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]FRAMELOWERING_H