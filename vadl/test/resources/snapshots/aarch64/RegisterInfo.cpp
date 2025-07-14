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
: processornamevalueGenRegisterInfo( processornamevalue::S30 )
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

markSuperRegs(Reserved, processornamevalue::S29); // frame pointer
markSuperRegs(Reserved, processornamevalue::S31); // stack pointer
markSuperRegs(Reserved, processornamevalue::); // global pointer


markSuperRegs(Reserved, processornamevalue::); // thread pointer




assert(checkAllSuperRegsMarked(Reserved));

return Reserved;
}


bool eliminateFrameIndexADDXI
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_ADDXI_imm12X_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDPSPre
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

if(Offset >= -64 && Offset <= 63 && AArch64Base_LDPSPre_offWSize_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDPSPst
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

if(Offset >= -64 && Offset <= 63 && AArch64Base_LDPSPst_offWSize_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDPWPre
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

if(Offset >= -64 && Offset <= 63 && AArch64Base_LDPWPre_offWSize_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDPWPst
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

if(Offset >= -64 && Offset <= 63 && AArch64Base_LDPWPst_offWSize_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDPXPre
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

if(Offset >= -64 && Offset <= 63 && AArch64Base_LDPXPre_offXSize_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDPXPst
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

if(Offset >= -64 && Offset <= 63 && AArch64Base_LDPXPst_offXSize_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreB
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreB_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreH
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreH_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreSBW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreSBW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreSBX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreSBX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreSHW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreSHW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreSHX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreSHX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreSWX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreSWX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPreX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPreX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstB
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstB_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstH
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstH_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstSBW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstSBW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstSBX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstSBX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstSHW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstSHW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstSHX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstSHX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstSWX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstSWX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexLDRPstX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_LDRPstX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPreB
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPreB_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPreH
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPreH_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPreW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPreW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPreX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPreX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPstB
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPstB_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPstH
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPstH_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPstW
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPstW_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
{
// the scratch register can properly be manipulated and used as address register.
FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
ImmOp.setImm( 0 );
return false; // success
}

return true; // failure
}
bool eliminateFrameIndexSTRPstX
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

if(Offset >= -256 && Offset <= 255 && AArch64Base_STRPstX_offset_predicate(Offset))
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

Register ScratchReg = MRI.createVirtualRegister(&processornamevalue::SRegClass);
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

case processornamevalue::ADDXI:
{
error = eliminateFrameIndexADDXI(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDPSPre:
{
error = eliminateFrameIndexLDPSPre(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDPSPst:
{
error = eliminateFrameIndexLDPSPst(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDPWPre:
{
error = eliminateFrameIndexLDPWPre(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDPWPst:
{
error = eliminateFrameIndexLDPWPst(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDPXPre:
{
error = eliminateFrameIndexLDPXPre(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDPXPst:
{
error = eliminateFrameIndexLDPXPst(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreB:
{
error = eliminateFrameIndexLDRPreB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreH:
{
error = eliminateFrameIndexLDRPreH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreSBW:
{
error = eliminateFrameIndexLDRPreSBW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreSBX:
{
error = eliminateFrameIndexLDRPreSBX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreSHW:
{
error = eliminateFrameIndexLDRPreSHW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreSHX:
{
error = eliminateFrameIndexLDRPreSHX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreSWX:
{
error = eliminateFrameIndexLDRPreSWX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreW:
{
error = eliminateFrameIndexLDRPreW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPreX:
{
error = eliminateFrameIndexLDRPreX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstB:
{
error = eliminateFrameIndexLDRPstB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstH:
{
error = eliminateFrameIndexLDRPstH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstSBW:
{
error = eliminateFrameIndexLDRPstSBW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstSBX:
{
error = eliminateFrameIndexLDRPstSBX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstSHW:
{
error = eliminateFrameIndexLDRPstSHW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstSHX:
{
error = eliminateFrameIndexLDRPstSHX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstSWX:
{
error = eliminateFrameIndexLDRPstSWX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstW:
{
error = eliminateFrameIndexLDRPstW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRPstX:
{
error = eliminateFrameIndexLDRPstX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPreB:
{
error = eliminateFrameIndexSTRPreB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPreH:
{
error = eliminateFrameIndexSTRPreH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPreW:
{
error = eliminateFrameIndexSTRPreW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPreX:
{
error = eliminateFrameIndexSTRPreX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPstB:
{
error = eliminateFrameIndexSTRPstB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPstH:
{
error = eliminateFrameIndexSTRPstH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPstW:
{
error = eliminateFrameIndexSTRPstW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRPstX:
{
error = eliminateFrameIndexSTRPstX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}

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
return TFI->hasFP(MF) ? processornamevalue::S29 /* FP */ : processornamevalue::S31 /* SP */;
}

const uint32_t * processornamevalueRegisterInfo::getCallPreservedMask(const MachineFunction & /*MF*/
, CallingConv::ID /*CC*/
) const
{
// defined in calling convention tablegen
return CSR_processornamevalue_RegMask;
}


/*static*/ unsigned processornamevalueRegisterInfo::S(unsigned index)
{
switch (index)
{

case 0:
return processornamevalue::S0;
case 1:
return processornamevalue::S1;
case 2:
return processornamevalue::S2;
case 3:
return processornamevalue::S3;
case 4:
return processornamevalue::S4;
case 5:
return processornamevalue::S5;
case 6:
return processornamevalue::S6;
case 7:
return processornamevalue::S7;
case 8:
return processornamevalue::S8;
case 9:
return processornamevalue::S9;
case 10:
return processornamevalue::S10;
case 11:
return processornamevalue::S11;
case 12:
return processornamevalue::S12;
case 13:
return processornamevalue::S13;
case 14:
return processornamevalue::S14;
case 15:
return processornamevalue::S15;
case 16:
return processornamevalue::S16;
case 17:
return processornamevalue::S17;
case 18:
return processornamevalue::S18;
case 19:
return processornamevalue::S19;
case 20:
return processornamevalue::S20;
case 21:
return processornamevalue::S21;
case 22:
return processornamevalue::S22;
case 23:
return processornamevalue::S23;
case 24:
return processornamevalue::S24;
case 25:
return processornamevalue::S25;
case 26:
return processornamevalue::S26;
case 27:
return processornamevalue::S27;
case 28:
return processornamevalue::S28;
case 29:
return processornamevalue::S29;
case 30:
return processornamevalue::S30;
case 31:
return processornamevalue::S31;

default:
{
std::string errMsg;
std::stringstream errMsgStream;
errMsgStream << "Unable to find index " << "'" << index << "'";
errMsgStream << " with name '«registerClass.simpleName»' !\n";
errMsg = errMsgStream.str();
report_fatal_error(errMsg.c_str());
}
}
}