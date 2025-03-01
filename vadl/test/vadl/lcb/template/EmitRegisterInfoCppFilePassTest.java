// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

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
        #include "processornamevalueRegisterInfo.h"
        #include "processornamevalueFrameLowering.h"
        #include "processornamevalueInstrInfo.h"
        #include "processornamevalueSubTarget.h"
        #include "Utils/processornamevalueBaseInfo.h"
        #include "Utils/ImmediateUtils.h"
        #include "MCTargetDesc/processornamevalueMCTargetDesc.h"
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
                
        #define DEBUG_TYPE "processornamevalueRegisterInfo"
                
        using namespace llvm;
                
        #define GET_REGINFO_TARGET_DESC
        #include "processornamevalueGenRegisterInfo.inc"
                
        void processornamevalueRegisterInfo::anchor() {}
                
        processornamevalueRegisterInfo::processornamevalueRegisterInfo()
            : processornamevalueGenRegisterInfo( processornamevalue::X1 )
        {
        }
                
        const uint16_t * processornamevalueRegisterInfo::getCalleeSavedRegs(const MachineFunction * /*MF*/
        ) const
        {
            // defined in calling convention tablegen
            return CSR_processornamevalue_SaveList;
        }
                
        BitVector processornamevalueRegisterInfo::getReservedRegs(const MachineFunction &MF) const
        {
            BitVector Reserved(getNumRegs());
                
            markSuperRegs(Reserved, processornamevalue::X8); // frame pointer
            markSuperRegs(Reserved, processornamevalue::X2); // stack pointer
            markSuperRegs(Reserved, processornamevalue::X3); // global pointer
            markSuperRegs(Reserved, processornamevalue::X4); // thread pointer
                
           \s
            markSuperRegs(Reserved,  processornamevalue::X0);
           \s
                
            assert(checkAllSuperRegsMarked(Reserved));
                
            return Reserved;
        }
                
                
        bool eliminateFrameIndexADDI
            ( MachineBasicBlock::iterator II
            , int SPAdj
            , unsigned FIOperandNum
            , unsigned FrameReg
            , StackOffset FrameIndexOffset
            , RegScavenger *RS
            )
        {
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
            if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
            {
                // the scratch register can properly be manipulated and used as address register.
                FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
                ImmOp.setImm( 0 );
                return false; // success
            }
                
            return true; // failure
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 1).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 1);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Itype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 2).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Stype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 2).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Stype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 2).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Stype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
            MachineInstr &MI = *II;
            assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
            assert(  MI.getOperand(FIOperandNum + 2).isImm() && "Immediate operand position does not match expected position!" );
                
            MachineOperand &FIOp = MI.getOperand(FIOperandNum);
            MachineOperand &ImmOp = MI.getOperand(FIOperandNum + 2);
                
            int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();
                
            //
            // try to inline the offset into the instruction
            //
                
            if(Offset >= -2048 && Offset <= 2047 && RV3264I_Stype_immS_predicate(Offset))
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
            const processornamevalueInstrInfo *TII = MF->getSubtarget<processornamevalueSubtarget>().getInstrInfo();
                
            //
            // try to generate a scratch register and adjust frame register with given offset
            //
                
            Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::XRegClass);
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
        bool processornamevalueRegisterInfo::eliminateFrameIndex(MachineBasicBlock::iterator II, int SPAdj, unsigned FIOperandNum, RegScavenger *RS) const
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
                case processornamevalue::ADDI:
                {
                  error = eliminateFrameIndexADDI(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LB:
                {
                  error = eliminateFrameIndexLB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LBU:
                {
                  error = eliminateFrameIndexLBU(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LD:
                {
                  error = eliminateFrameIndexLD(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LH:
                {
                  error = eliminateFrameIndexLH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LHU:
                {
                  error = eliminateFrameIndexLHU(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LW:
                {
                  error = eliminateFrameIndexLW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::LWU:
                {
                  error = eliminateFrameIndexLWU(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::SB:
                {
                  error = eliminateFrameIndexSB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::SD:
                {
                  error = eliminateFrameIndexSD(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::SH:
                {
                  error = eliminateFrameIndexSH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
                  break;
                }
                case processornamevalue::SW:
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
                
        Register processornamevalueRegisterInfo::getFrameRegister(const MachineFunction &MF) const
        {
            const TargetFrameLowering *TFI = getFrameLowering(MF);
            return TFI->hasFP(MF) ? processornamevalue::X8 /* FP */ : processornamevalue::X2 /* SP */;
        }
                
        const uint32_t * processornamevalueRegisterInfo::getCallPreservedMask(const MachineFunction & /*MF*/
                                                                           , CallingConv::ID /*CC*/
        ) const
        {
            // defined in calling convention tablegen
            return CSR_processornamevalue_RegMask;
        }
                
                
        /*static*/ unsigned processornamevalueRegisterInfo::X(unsigned index)
        {
          switch (index)
          {
         \s
            case 0:
                return processornamevalue::X0;
            case 1:
                return processornamevalue::X1;
            case 2:
                return processornamevalue::X2;
            case 3:
                return processornamevalue::X3;
            case 4:
                return processornamevalue::X4;
            case 5:
                return processornamevalue::X5;
            case 6:
                return processornamevalue::X6;
            case 7:
                return processornamevalue::X7;
            case 8:
                return processornamevalue::X8;
            case 9:
                return processornamevalue::X9;
            case 10:
                return processornamevalue::X10;
            case 11:
                return processornamevalue::X11;
            case 12:
                return processornamevalue::X12;
            case 13:
                return processornamevalue::X13;
            case 14:
                return processornamevalue::X14;
            case 15:
                return processornamevalue::X15;
            case 16:
                return processornamevalue::X16;
            case 17:
                return processornamevalue::X17;
            case 18:
                return processornamevalue::X18;
            case 19:
                return processornamevalue::X19;
            case 20:
                return processornamevalue::X20;
            case 21:
                return processornamevalue::X21;
            case 22:
                return processornamevalue::X22;
            case 23:
                return processornamevalue::X23;
            case 24:
                return processornamevalue::X24;
            case 25:
                return processornamevalue::X25;
            case 26:
                return processornamevalue::X26;
            case 27:
                return processornamevalue::X27;
            case 28:
                return processornamevalue::X28;
            case 29:
                return processornamevalue::X29;
            case 30:
                return processornamevalue::X30;
            case 31:
                return processornamevalue::X31;
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
