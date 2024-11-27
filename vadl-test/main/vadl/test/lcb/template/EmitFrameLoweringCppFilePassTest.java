package vadl.test.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitFrameLoweringCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitFrameLoweringCppFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitFrameLoweringCppFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitFrameLoweringCppFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #include "rv64imFrameLowering.h"
        #include "rv64im.h"
        #include "rv64imSubTarget.h"
        #include "MCTargetDesc/rv64imMCTargetDesc.h"
        #include "Utils/rv64imBaseInfo.h"
        #include "rv64imMachineFunctionInfo.h"
        #include "llvm/CodeGen/MachineFrameInfo.h"
        #include "llvm/CodeGen/MachineFunction.h"
        #include "llvm/CodeGen/MachineInstrBuilder.h"
        #include "llvm/CodeGen/MachineRegisterInfo.h"
        #include "llvm/CodeGen/RegisterScavenging.h"
        #include "rv64imInstrInfo.h"
        #include <sstream>
        #include <string>
        
        #define DEBUG_TYPE "rv64imFrameLowering"
        
        using namespace llvm;
        
        void rv64imFrameLowering::anchor() {}
        
        rv64imFrameLowering::rv64imFrameLowering(const rv64imSubtarget &STI)
            : TargetFrameLowering(StackGrowsDown, Align(16) /*=StackAlignment*/
                                  ,
                                  0 /*=LocalAreaOffset*/
                                  ),
              STI(STI)
        {
        }
        
        // Not preserve stack space within prologue for outgoing variables when the
        // function contains variable size objects and let eliminateCallFramePseudoInstr
        // preserve stack space for it.
        bool rv64imFrameLowering::hasReservedCallFrame(const MachineFunction &MF) const
        {
            return !MF.getFrameInfo().hasVarSizedObjects();
        }
        
        // Returns the stack size, rounded back
        // up to the required stack alignment.
        uint64_t rv64imFrameLowering::getStackSize(const MachineFunction &MF) const
        {
            const MachineFrameInfo &MFI = MF.getFrameInfo();
            auto *RVFI = MF.getInfo<rv64imMachineFunctionInfo>();
            return alignTo(MFI.getStackSize(), getStackAlign());
        }
        
        // Eliminate ADJCALLSTACKDOWN, ADJCALLSTACKUP pseudo instructions.
        MachineBasicBlock::iterator rv64imFrameLowering::eliminateCallFramePseudoInstr(MachineFunction &MF, MachineBasicBlock &MBB, MachineBasicBlock::iterator MI) const
        {
            const rv64imInstrInfo *TII = STI.getInstrInfo();
        
            // savely erases the pseudo instructions for building up
            // and tearing down the call stack
            if (MI->getOpcode() == rv64im::ADJCALLSTACKUP ||
                MI->getOpcode() == rv64im::ADJCALLSTACKDOWN)
            {
        
                Register SPReg = rv64im::X2;
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
        
                        if (MI->getOpcode() == rv64im::ADJCALLSTACKDOWN)
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
        
        bool rv64imFrameLowering::hasFP(const MachineFunction &MF) const
        {
           \s
            const TargetRegisterInfo *RegInfo = MF.getSubtarget().getRegisterInfo();
            const MachineFrameInfo &MFI = MF.getFrameInfo();
                return MF.getTarget().Options.DisableFramePointerElim(MF) ||
                       RegInfo->hasStackRealignment(MF) ||
                       MFI.hasVarSizedObjects() ||
                       MFI.isFrameAddressTaken();
           \s
           \s
        }
        
        void rv64imFrameLowering::emitPrologue(MachineFunction &MF, MachineBasicBlock &MBB) const
        {
            MachineFrameInfo &MFI = MF.getFrameInfo();
            auto *FI = MF.getInfo<rv64imMachineFunctionInfo>();
            const rv64imInstrInfo *TII = STI.getInstrInfo();
            MachineBasicBlock::iterator MBBI = MBB.begin();
        
           \s
                Register FPReg = rv64im::X8;
           \s
            Register SPReg = rv64im::X2;
        
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
           \s
            // Generate new FP.
            if (hasFP(MF))
            {
                if (TII->adjustReg(MBB, MBBI, DL, FPReg, SPReg, StackSize - FI->getVarArgsSaveSize(), MachineInstr::FrameSetup))
                {
                    llvm_unreachable("unable to adjust and generate frame pointer in 'emitPrologue'!");
                }
            }
           \s
        }
        
        void rv64imFrameLowering::emitEpilogue(MachineFunction &MF, MachineBasicBlock &MBB) const
        {
            const rv64imInstrInfo *TII = STI.getInstrInfo();
            MachineFrameInfo &MFI = MF.getFrameInfo();
            auto *FI = MF.getInfo<rv64imMachineFunctionInfo>();
        
           \s
                Register FPReg = rv64im::X8;
           \s
            Register SPReg = rv64im::X2;
        
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
        
           \s
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
           \s
        
            // Deallocate stack
            if (TII->adjustReg(MBB, MBBI, DL, SPReg, SPReg, StackSize, MachineInstr::FrameDestroy))
            {
                llvm_unreachable("unable to adjust stack pointer for stack deallocation in 'emitEpilogue'!");
            }
        }
        
        void rv64imFrameLowering::determineCalleeSaves(MachineFunction &MF, BitVector &SavedRegs, RegScavenger *RS) const
        {
            // Determine actual callee saved registers that need to be saved
            TargetFrameLowering::determineCalleeSaves(MF, SavedRegs, RS);
           \s
            // If frame pointer is present save both return address and frame pointer
            if (hasFP(MF))
            {
                SavedRegs.set( rv64im::X1 ); // return address
                SavedRegs.set(  rv64im::X8 );  // frame pointer
            }
           \s
        }
        
        bool rv64imFrameLowering::spillCalleeSavedRegisters(MachineBasicBlock &MBB, MachineBasicBlock::iterator MI, ArrayRef<CalleeSavedInfo> CSI, const TargetRegisterInfo *TRI) const
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
        
        bool rv64imFrameLowering::restoreCalleeSavedRegisters(MachineBasicBlock &MBB, MachineBasicBlock::iterator MI, MutableArrayRef<CalleeSavedInfo> CSI, const TargetRegisterInfo *TRI) const
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
        StackOffset rv64imFrameLowering::getFrameIndexReference(const MachineFunction &MF, int FI, Register &FrameReg) const
        {
            const MachineFrameInfo &MFI = MF.getFrameInfo();
            const TargetRegisterInfo *RI = MF.getSubtarget().getRegisterInfo();
            const auto *RVFI = MF.getInfo<rv64imMachineFunctionInfo>();
        
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
                FrameReg = rv64im::X2;
                Offset += StackOffset::getFixed(StackSize);
            }
            else if (RI->hasStackRealignment(MF) && !MFI.isFixedObjectIndex(FI))
            {
                // If the stack was realigned, the frame pointer is set in order to allow
                // SP to be restored, so we need another base register to record the stack
                // after realignment.
                // TODO: @chochrainer RISCV uses base register
        
                FrameReg = rv64im::X2;
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
        """.trim().lines(), output);
  }
}
