#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]INSTRINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]INSTRINFO_H

#include "llvm/CodeGen/TargetInstrInfo.h"

#define GET_INSTRINFO_HEADER
#include "[(${namespace})]GenInstrInfo.inc"

namespace llvm
{
    class [(${namespace})]Subtarget;

    namespace [(${namespace})]CC {
      enum CondCode {
        COND_EQ,
        COND_NE,
        COND_LT,
        COND_GE,
        COND_LTU,
        COND_GEU,
        COND_INVALID
      };

      CondCode getOppositeBranchCondition(CondCode);
      CondCode getCondFromBranchOpc(unsigned Opc);
    }

    class [(${namespace})]InstrInfo : public [(${namespace})]GenInstrInfo
    {
        // virtual anchor method to decrease link time as the vtable
        virtual void anchor();

        public:
            [(${namespace})]InstrInfo( [(${namespace})]Subtarget &STI );

            void copyPhysReg
                ( MachineBasicBlock &MBB
                , MachineBasicBlock::iterator MBBI
                , const DebugLoc &DL
                , MCRegister DestReg
                , MCRegister SrcReg
                , bool KillSrc
                ) const override;

            void storeRegToStackSlot
                ( MachineBasicBlock &MBB
                , MachineBasicBlock::iterator MBBI
                , Register SrcReg
                , bool IsKill
                , int FrameIndex
                , const TargetRegisterClass *RC
                , const TargetRegisterInfo *TRI
                , Register VReg
                ) const override;

            void loadRegFromStackSlot
                ( MachineBasicBlock &MBB
                , MachineBasicBlock::iterator MBBI
                , Register DestReg
                , int FrameIndex
                , const TargetRegisterClass *RC
                , const TargetRegisterInfo *TRI
                , Register VReg
                ) const override;

            bool adjustReg
                ( MachineBasicBlock &MBB
                , MachineBasicBlock::iterator MBBI
                , const DebugLoc &DL
                , Register DestReg
                , Register SrcReg
                , int64_t Val
                , MachineInstr::MIFlag Flag = MachineInstr::NoFlags
                ) const;

            MachineBasicBlock *getBranchDestBlock(const MachineInstr &MI) const override;

            bool analyzeBranch(MachineBasicBlock &MBB, MachineBasicBlock *&TBB,
                                 MachineBasicBlock *&FBB,
                                 SmallVectorImpl<MachineOperand> &Cond,
                                 bool AllowModify) const override;

            bool isBranchOffsetInRange(unsigned BranchOpc, int64_t BrOffset) const override;

            unsigned getInstSizeInBytes(const MachineInstr &MI) const override;

            unsigned insertBranch(MachineBasicBlock &MBB, MachineBasicBlock *TBB,
                                    MachineBasicBlock *FBB, ArrayRef<MachineOperand> Cond,
                                    const DebugLoc &dl,
                                    int *BytesAdded = nullptr) const override;

            const MCInstrDesc &getBrCond([(${namespace})]CC::CondCode CC) const;

        private:
            const [(${namespace})]Subtarget &STI;
    };
}

#endif