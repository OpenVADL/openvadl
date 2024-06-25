#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]REGISTERINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]REGISTERINFO_H

#include "llvm/CodeGen/TargetRegisterInfo.h"
#include <string>

#define GET_REGINFO_HEADER
#include "[(${namespace})]GenRegisterInfo.inc"

namespace llvm
{
    struct [(${namespace})]RegisterInfo : public [(${namespace})]GenRegisterInfo
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();

        [(${namespace})]RegisterInfo();

        const uint32_t *getCallPreservedMask(const MachineFunction &MF, CallingConv::ID) const override;

        const uint16_t *getCalleeSavedRegs(const MachineFunction *MF = nullptr) const override;

        BitVector getReservedRegs(const MachineFunction &MF) const override;

        bool requiresRegisterScavenging(const MachineFunction &MF) const override
        {
            return true;
        }

        bool requiresFrameIndexScavenging(const MachineFunction &MF) const override
        {
            return true;
        }

        bool trackLivenessAfterRegAlloc(const MachineFunction &) const override
        {
            return true;
        }

        bool useFPForScavengingIndex(const MachineFunction &MF) const override
        {
            return false;
        }

        bool eliminateFrameIndex(MachineBasicBlock::iterator II, int SPAdj,
                                 unsigned FIOperandNum,
                                 RegScavenger *RS = nullptr) const override;

        «FOR registerClass : registerClasses» static unsigned «registerClass.simpleName»(unsigned index);
        «ENDFOR» static unsigned registerOpcodeLookup(std::string className, unsigned index);

        Register getFrameRegister(const MachineFunction &MF) const override;
    };
} // end namespace llvm

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]REGISTERINFO_H