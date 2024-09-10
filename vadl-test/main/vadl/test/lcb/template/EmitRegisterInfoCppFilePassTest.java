package vadl.test.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitRegisterInfoCppFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitRegisterInfoCppFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitRegisterInfoCppFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #include "rv64imRegisterInfo.h"
        #include "rv64imFrameLowering.h"
        #include "rv64imInstrInfo.h"
        #include "rv64imSubtarget.h"
        #include "Utils/rv64imBaseInfo.h"
        #include "Utils/ImmediateUtils.h"
        #include "MCTargetDesc/rv64imMCTargetDesc.h"
        #include "llvm/ADT/BitVector.h"
        #include "llvm/ADT/STLExtras.h"
        #include "llvm/CodeGen/MachineFrameInfo.h"
        #include "llvm/CodeGen/MachineFunction.h"
        #include "llvm/CodeGen/MachineInstrBuilder.h"
        #include "llvm/CodeGen/RegisterScavenging.h"
        #include "llvm/CodeGen/TargetFrameLowering.h"
        #include "llvm/CodeGen/TargetInstrInfo.h"
        #include "llvm/IR/Function.h"
        #include "llvm/IR/Type.h"
        #include "llvm/Support/ErrorHandling.h"
        #include "llvm/Support/Debug.h"
        #include <iostream>
        #include <sstream>
                
        #define DEBUG_TYPE "rv64imRegisterInfo"
                
        using namespace llvm;
                
        #define GET_REGINFO_TARGET_DESC
        #include "rv64imGenRegisterInfo.inc"
                
        void rv64imRegisterInfo::anchor() {}
                
        rv64imRegisterInfo::rv64imRegisterInfo()
            : rv64imGenRegisterInfo( «emitWithNamespace(returnAddress)» )
        {
        }
                
        const uint16_t * rv64imRegisterInfo::getCalleeSavedRegs(const MachineFunction * /*MF*/
        ) const
        {
            // defined in calling convention tablegen
            return CSR_rv64im_SaveList;
        }
                
        BitVector rv64imRegisterInfo::getReservedRegs(const MachineFunction &MF) const
        {
            BitVector Reserved(getNumRegs());
                
            markSuperRegs(Reserved, rv64im::X8); // frame pointer
            markSuperRegs(Reserved, rv64im::X2); // stack pointer
            markSuperRegs(Reserved, rv64im::X3); // global pointer
                
            // TODO: Add constant registers
            assert(checkAllSuperRegsMarked(Reserved));
                
            return Reserved;
        }
                
                
        bool eliminateFrameIndexLB
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexLBU
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexLD
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexLH
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexLHU
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexLW
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexLWU
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexSB
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexSD
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexSH
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
        bool eliminateFrameIndexSW
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            // sanity check for generated code
            assert( FIOperandNum == 1 && "Frame Index operand position does not match expected position!" );
                
            MachineInstr &MI = *II;
            MachineOperand &FIOp = MI.getOperand(1);
            MachineOperand &ImmOp = MI.getOperand(2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(immS_predicate_predicate(Offset))
            {
                // immediate can be encoded and instruction can be inlined.
                FIOp.ChangeToRegister( FrameReg, false /* isDef */ );
                ImmOp.setImm( Offset );
                return false; // success
            }
                
                
            DebugLoc DL = MI.getDebugLoc();
            MachineBasicBlock &MBB = *MI.getParent();
            MachineFunction *MF = MBB.getParent();
            MachineRegisterInfo &MRI = MF->getRegInfo();
            const CPUInstrInfo *TII = MF->getSubtarget<CPUSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&CPU::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
        }
                
                
        /**
         * This method calls its own replacement class for each allowed instruction.
         * Inside the special instruction the following steps or tries to remove the FI are done.
         *
         *     1. try to inline the frame index calculation into the current instruction.
         *     2. check if we can move the immediate materialization and frame index addition
         *        into a separate instruction with scratch register. The scratch register must be
         *        useable with our current instruction.
         *     3. replace the current instruction with
         *        3.1 a more specific instruction that can load the offset
         *        3.2 a very general instruction that uses a scratch register for computing the
         *            desired frame index.
         *
         * If an instruction is not supported, an llvm_fatal_error is emitted as it should be impossible
         * for a frame index to be an operand.
         */
        bool rv64imRegisterInfo::eliminateFrameIndex(MachineBasicBlock::iterator II, int SPAdj, unsigned FIOperandNum, RegScavenger *RS) const
        {
            MachineInstr &MI = *II;
            const MachineFunction &MF = *MI.getParent()->getParent();
                
            const TargetInstrInfo *TII = MF.getSubtarget().getInstrInfo();
            const std::string mnemonic = TII->getName(MI.getOpcode()).str(); // for debug purposes
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            unsigned FI = FIOp.getIndex();
            Register FrameReg;
            StackOffset FrameIndexOffset = getFrameLowering(MF)->getFrameIndexReference(MF, FI, FrameReg);
                
            bool error = true;
            switch (MI.getOpcode())
            {
               \s
                case rv64im::LB:
                {
                  error = eliminateFrameIndexLB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::LBU:
                {
                  error = eliminateFrameIndexLBU(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::LD:
                {
                  error = eliminateFrameIndexLD(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::LH:
                {
                  error = eliminateFrameIndexLH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::LHU:
                {
                  error = eliminateFrameIndexLHU(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::LW:
                {
                  error = eliminateFrameIndexLW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::LWU:
                {
                  error = eliminateFrameIndexLWU(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::SB:
                {
                  error = eliminateFrameIndexSB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::SD:
                {
                  error = eliminateFrameIndexSD(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::SH:
                {
                  error = eliminateFrameIndexSH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case rv64im::SW:
                {
                  error = eliminateFrameIndexSW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
               \s
                default:
                {
                    /* This should be unreachable! */
                    std::string errMsg;
                    std::stringstream errMsgStream;
                    errMsgStream << "Unexpected frame index for instruction '" << mnemonic << "'";
                    errMsg = errMsgStream.str();
                    llvm_unreachable(errMsg.c_str());
                }
            }
                
            if (error) // something went wrong
            {
                std::string errMsg;
                std::stringstream errMsgStream;
                errMsgStream << "Unable to eliminate frame index ('FrameIndex<" << FrameIndexOffset.getFixed() << ">')";
                errMsgStream << " for instruction '" << mnemonic << "'";
                errMsg = errMsgStream.str();
                report_fatal_error(errMsg.c_str()); // if we cannot eliminate the frame index abort!
            }
                
            return true;
        }
                
        Register rv64imRegisterInfo::getFrameRegister(const MachineFunction &MF) const
        {
            const TargetFrameLowering *TFI = getFrameLowering(MF);
            return TFI->hasFP(MF) ? X8 /* FP */ : X2 /* SP */;
        }
                
        const uint32_t * rv64imRegisterInfo::getCallPreservedMask(const MachineFunction & /*MF*/
                                                                           , CallingConv::ID /*CC*/
        ) const
        {
            // defined in calling convention tablegen
            return CSR_rv64im_RegMask;
        }
                
                
        /*static*/ unsigned rv64imRegisterInfo::X(unsigned index)
        {
          switch (index)
          {
         \s
            case 0:
                return rv64im::X0;
            case 1:
                return rv64im::X1;
            case 2:
                return rv64im::X2;
            case 3:
                return rv64im::X3;
            case 4:
                return rv64im::X4;
            case 5:
                return rv64im::X5;
            case 6:
                return rv64im::X6;
            case 7:
                return rv64im::X7;
            case 8:
                return rv64im::X8;
            case 9:
                return rv64im::X9;
            case 10:
                return rv64im::X10;
            case 11:
                return rv64im::X11;
            case 12:
                return rv64im::X12;
            case 13:
                return rv64im::X13;
            case 14:
                return rv64im::X14;
            case 15:
                return rv64im::X15;
            case 16:
                return rv64im::X16;
            case 17:
                return rv64im::X17;
            case 18:
                return rv64im::X18;
            case 19:
                return rv64im::X19;
            case 20:
                return rv64im::X20;
            case 21:
                return rv64im::X21;
            case 22:
                return rv64im::X22;
            case 23:
                return rv64im::X23;
            case 24:
                return rv64im::X24;
            case 25:
                return rv64im::X25;
            case 26:
                return rv64im::X26;
            case 27:
                return rv64im::X27;
            case 28:
                return rv64im::X28;
            case 29:
                return rv64im::X29;
            case 30:
                return rv64im::X30;
            case 31:
                return rv64im::X31;
         \s
            default:
            {
                std::string errMsg;
                std::stringstream errMsgStream;
                errMsgStream << "Unable to find index " << "'" << index << "'";
                errMsgStream << " with name '«registerClass.simpleName»' !\\n";
                errMsg = errMsgStream.str();
                report_fatal_error(errMsg.c_str());
            }
          }
        }
        """.trim().lines(), output);
  }
}
