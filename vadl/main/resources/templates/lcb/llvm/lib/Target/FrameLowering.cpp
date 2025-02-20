#include "[(${namespace})]FrameLowering.h"
#include "[(${namespace})].h"
#include "[(${namespace})]SubTarget.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "[(${namespace})]MachineFunctionInfo.h"
#include "llvm/CodeGen/MachineFrameInfo.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/CodeGen/MachineInstrBuilder.h"
#include "llvm/CodeGen/MachineRegisterInfo.h"
#include "llvm/CodeGen/RegisterScavenging.h"
#include "[(${namespace})]InstrInfo.h"
#include <sstream>
#include <string>

#define DEBUG_TYPE "[(${namespace})]FrameLowering"

using namespace llvm;

void [(${namespace})]FrameLowering::anchor() {}

[(${namespace})]FrameLowering::[(${namespace})]FrameLowering(const [(${namespace})]Subtarget &STI)
    : TargetFrameLowering(StackGrowsDown,
      Align([(${stackAlignment})]) /*=StackAlignment*/,
      0 /*=LocalAreaOffset*/,
      Align([(${transientStackAlignment})]) /*=TransientStackAlignment*/
                          ),
      STI(STI)
{
}

// Not preserve stack space within prologue for outgoing variables when the
// function contains variable size objects and let eliminateCallFramePseudoInstr
// preserve stack space for it.
bool [(${namespace})]FrameLowering::hasReservedCallFrame(const MachineFunction &MF) const
{
    return !MF.getFrameInfo().hasVarSizedObjects();
}

// Returns the stack size, rounded back
// up to the required stack alignment.
uint64_t [(${namespace})]FrameLowering::getStackSize(const MachineFunction &MF) const
{
    const MachineFrameInfo &MFI = MF.getFrameInfo();
    auto *RVFI = MF.getInfo<[(${namespace})]MachineFunctionInfo>();
    return alignTo(MFI.getStackSize(), getStackAlign());
}

// Eliminate ADJCALLSTACKDOWN, ADJCALLSTACKUP pseudo instructions.
MachineBasicBlock::iterator [(${namespace})]FrameLowering::eliminateCallFramePseudoInstr(MachineFunction &MF, MachineBasicBlock &MBB, MachineBasicBlock::iterator MI) const
{
    const [(${namespace})]InstrInfo *TII = STI.getInstrInfo();

    // savely erases the pseudo instructions for building up
    // and tearing down the call stack
    if (MI->getOpcode() == [(${namespace})]::ADJCALLSTACKUP ||
        MI->getOpcode() == [(${namespace})]::ADJCALLSTACKDOWN)
    {

        Register SPReg = [(${namespace})]::[(${stackPointer})];
        DebugLoc DL = MI->getDebugLoc();

        if (!hasReservedCallFrame(MF))
        {
            // If space has not been reserved for a call frame, ADJCALLSTACKDOWN and
            // ADJCALLSTACKUP must be converted to instructions manipulating the stack
            // pointer. This is necessary when there is a variable length stack
            // allocation (e.g. alloca), which means it's not possible to allocate
            // space for outgoing arguments from within the function prologue.

            int64_t Amount = MI->getOperand(0).getImm();

            if (Amount != 0)
            {
                // Ensure the stack remains aligned after adjustment.
                Amount = alignSPAdjust(Amount);

                if (MI->getOpcode() == [(${namespace})]::ADJCALLSTACKDOWN)
                {
                    Amount = -Amount;
                }

                if (TII->adjustReg(MBB, MI, DL, SPReg, SPReg, Amount, MachineInstr::NoFlags))
                {
                    llvm_unreachable("unable to adjust stack pointer with value in 'eliminateCallFramePseudoInstr'!");
                }
            }
        }

        return MBB.erase(MI);
    }

    std::stringstream ss;
    ss << "unknown opcode to eliminate call frame '" << TII->getName(MI->getOpcode()).str() << "'";
    std::string errorMsg = ss.str();
    llvm_unreachable(errorMsg.c_str());
}

bool [(${namespace})]FrameLowering::hasFP(const MachineFunction &MF) const
{
    [#th:block th:if="${hasFramePointer}"]
    const TargetRegisterInfo *RegInfo = MF.getSubtarget().getRegisterInfo();
    const MachineFrameInfo &MFI = MF.getFrameInfo();
        return MF.getTarget().Options.DisableFramePointerElim(MF) ||
               RegInfo->hasStackRealignment(MF) ||
               MFI.hasVarSizedObjects() ||
               MFI.isFrameAddressTaken();
    [/th:block]
    [#th:block th:if="${!hasFramePointer}"]
    return false;
    [/th:block]
}

void [(${namespace})]FrameLowering::emitPrologue(MachineFunction &MF, MachineBasicBlock &MBB) const
{
    MachineFrameInfo &MFI = MF.getFrameInfo();
    auto *FI = MF.getInfo<[(${namespace})]MachineFunctionInfo>();
    const [(${namespace})]InstrInfo *TII = STI.getInstrInfo();
    MachineBasicBlock::iterator MBBI = MBB.begin();

    [#th:block th:if="${hasFramePointer}"]
        Register FPReg = [(${namespace})]::[(${framePointer})];
    [/th:block]
    Register SPReg = [(${namespace})]::[(${stackPointer})];

    // Debug location must be unknown since the first debug location is used
    // to determine the end of the prologue.
    DebugLoc DL;

    uint64_t StackSize = getStackSize(MF);

    // Early exit if there is no need to allocate on the stack
    if (StackSize == 0 && !MFI.adjustsStack())
    {
        return;
    }

    // Allocate space on the stack if necessary.
    if (TII->adjustReg(MBB, MBBI, DL, SPReg, SPReg, -StackSize, MachineInstr::FrameSetup))
    {
        llvm_unreachable("unable to adjust stack pointer with stack size in 'emitPrologue'!");
    }

    // Advance to after the callee/caller saved register spills to adjust the frame pointer
    const std::vector<CalleeSavedInfo> &CSI = MFI.getCalleeSavedInfo();
    std::advance(MBBI, CSI.size());
    [#th:block th:if="${hasFramePointer}"]
    // Generate new FP.
    if (hasFP(MF))
    {
        if (TII->adjustReg(MBB, MBBI, DL, FPReg, SPReg, StackSize - FI->getVarArgsSaveSize(), MachineInstr::FrameSetup))
        {
            llvm_unreachable("unable to adjust and generate frame pointer in 'emitPrologue'!");
        }
    }
    [/th:block]
}

void [(${namespace})]FrameLowering::emitEpilogue(MachineFunction &MF, MachineBasicBlock &MBB) const
{
    const [(${namespace})]InstrInfo *TII = STI.getInstrInfo();
    MachineFrameInfo &MFI = MF.getFrameInfo();
    auto *FI = MF.getInfo<[(${namespace})]MachineFunctionInfo>();

    [#th:block th:if="${hasFramePointer}"]
        Register FPReg = [(${namespace})]::[(${framePointer})];
    [/th:block]
    Register SPReg = [(${namespace})]::[(${stackPointer})];

    // Get the insert location for the epilogue. If there were no terminators in
    // the block, get the last instruction.
    MachineBasicBlock::iterator MBBI = MBB.end();
    DebugLoc DL;

    if (!MBB.empty())
    {
        MBBI = MBB.getFirstTerminator();
        if (MBBI == MBB.end())
        {
            MBBI = MBB.getLastNonDebugInstr();
        }

        DL = MBBI->getDebugLoc();

        // If this is not a terminator, the actual insert location should be after the
        // last instruction.
        if (!MBBI->isTerminator())
        {
            MBBI = std::next(MBBI);
        }
    }

    uint64_t StackSize = getStackSize(MF);
    StackSize = alignTo(StackSize, getStackAlign());

    const auto &CSI = MFI.getCalleeSavedInfo();

    // Skip to before the restores of callee-saved registers
    // TODO: @chochrainer FIXME: assumes exactly one instruction is used to restore each
    // callee-saved register.
    auto LastFrameDestroy = MBBI;
    if (!CSI.empty())
    {
        LastFrameDestroy = std::prev(MBBI, CSI.size());
    }

    [#th:block th:if="${hasFramePointer}"]
    // Restore the stack pointer using the value of the frame pointer.
    if (hasFP(MF) && MFI.hasVarSizedObjects())
    {
        assert(hasFP(MF) && "frame pointer should not have been eliminated");
        uint64_t FPOffset = StackSize - FI->getVarArgsSaveSize();
        if (TII->adjustReg(MBB, LastFrameDestroy, DL, SPReg, FPReg, -FPOffset, MachineInstr::FrameDestroy))
        {
            llvm_unreachable("unable to adjust stack pointer with value in 'emitEpilogue'!");
        }
    }
    [/th:block]

    // Deallocate stack
    if (TII->adjustReg(MBB, MBBI, DL, SPReg, SPReg, StackSize, MachineInstr::FrameDestroy))
    {
        llvm_unreachable("unable to adjust stack pointer for stack deallocation in 'emitEpilogue'!");
    }
}

void [(${namespace})]FrameLowering::determineCalleeSaves(MachineFunction &MF, BitVector &SavedRegs, RegScavenger *RS) const
{
    // Determine actual callee saved registers that need to be saved
    TargetFrameLowering::determineCalleeSaves(MF, SavedRegs, RS);
    [#th:block th:if="${hasFramePointer}"]
    // If frame pointer is present save both return address and frame pointer
    if (hasFP(MF))
    {
        SavedRegs.set( [(${namespace})]::[(${returnAddress})] ); // return address
        SavedRegs.set(  [(${namespace})]::[(${framePointer})] );  // frame pointer
    }
    [/th:block]
}

bool [(${namespace})]FrameLowering::spillCalleeSavedRegisters(MachineBasicBlock &MBB, MachineBasicBlock::iterator MI, ArrayRef<CalleeSavedInfo> CSI, const TargetRegisterInfo *TRI) const
{
    if (CSI.empty())
    {
        return true;
    }

    MachineFunction *MF = MBB.getParent();
    const TargetInstrInfo &TII = *MF->getSubtarget().getInstrInfo();

    for (auto &CS : CSI)
    {
        Register Reg = CS.getReg();
        const TargetRegisterClass *RC = TRI->getMinimalPhysRegClass(Reg);
        TII.storeRegToStackSlot(MBB, MI, Reg, true, CS.getFrameIdx(), RC, TRI, Register());
    }

    return true;
}

bool [(${namespace})]FrameLowering::restoreCalleeSavedRegisters(MachineBasicBlock &MBB, MachineBasicBlock::iterator MI, MutableArrayRef<CalleeSavedInfo> CSI, const TargetRegisterInfo *TRI) const
{
    if (CSI.empty())
    {
        return true;
    }

    MachineFunction *MF = MBB.getParent();
    const TargetInstrInfo &TII = *MF->getSubtarget().getInstrInfo();

    for (auto &CS : reverse(CSI))
    {
        Register Reg = CS.getReg();
        const TargetRegisterClass *RC = TRI->getMinimalPhysRegClass(Reg);
        TII.loadRegFromStackSlot(MBB, MI, Reg, CS.getFrameIdx(), RC, TRI, Register());
    }

    return true;
}

// TODO: @chochrainer improve and special handling
StackOffset [(${namespace})]FrameLowering::getFrameIndexReference(const MachineFunction &MF, int FI, Register &FrameReg) const
{
    const MachineFrameInfo &MFI = MF.getFrameInfo();
    const TargetRegisterInfo *RI = MF.getSubtarget().getRegisterInfo();
    const auto *RVFI = MF.getInfo<[(${namespace})]MachineFunctionInfo>();

    // Callee-saved registers should be referenced relative to the stack
    // pointer (positive offset), otherwise use the frame pointer (negative
    // offset).
    const std::vector<CalleeSavedInfo> &CSI = MFI.getCalleeSavedInfo();
    int MinCSFI = 0;
    int MaxCSFI = -1;

    // get start and end of callee saved registers
    if (CSI.size())
    {
        MinCSFI = CSI[0].getFrameIdx();
        MaxCSFI = CSI[CSI.size() - 1].getFrameIdx();
    }

    // TODO @chochrainer:
    //    * deal with other offset values
    //    * deal with split stack pointer adjustments
    assert(getOffsetOfLocalArea() == 0 && "cannot deal with local area offset");
    assert(MFI.getOffsetAdjustment() == 0 && "cannot deal with offset adjustments");

    StackOffset Offset = StackOffset::getFixed(MFI.getObjectOffset(FI));
    auto StackSize = getStackSize(MF);
    StackSize = alignTo(StackSize, getStackAlign());

    if (FI >= MinCSFI && FI <= MaxCSFI)
    {
        // use the stack pointer for callee saved register
        FrameReg = [(${namespace})]::[(${stackPointer})];
        Offset += StackOffset::getFixed(StackSize);
    }
    else if (RI->hasStackRealignment(MF) && !MFI.isFixedObjectIndex(FI))
    {
        // If the stack was realigned, the frame pointer is set in order to allow
        // SP to be restored, so we need another base register to record the stack
        // after realignment.
        // TODO: @chochrainer RISCV uses base register

        FrameReg = [(${namespace})]::[(${stackPointer})];
        Offset += StackOffset::getFixed(StackSize);
    }
    else
    {
        FrameReg = RI->getFrameRegister(MF);
        if (hasFP(MF))
        {
            Offset += StackOffset::getFixed(RVFI->getVarArgsSaveSize());
        }
        else
        {
            Offset += StackOffset::getFixed(StackSize);
        }
    }

    return Offset;
}