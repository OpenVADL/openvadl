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

markSuperRegs(Reserved, processornamevalue::X29); // frame pointer
markSuperRegs(Reserved, processornamevalue::S31); // stack pointer





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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_ADDWI_imm12X_predicate(Offset))
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
bool eliminateFrameIndexLDRB
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRH
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRSBW
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRSBX
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRSHW
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRSHX
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRSWX
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRW
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDRX
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexLDURB
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
bool eliminateFrameIndexLDURH
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
bool eliminateFrameIndexLDURSBW
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
bool eliminateFrameIndexLDURSBX
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
bool eliminateFrameIndexLDURSHW
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
bool eliminateFrameIndexLDURSHX
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
bool eliminateFrameIndexLDURSWX
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
bool eliminateFrameIndexLDURW
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
bool eliminateFrameIndexLDURX
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
bool eliminateFrameIndexSTRB
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexSTRH
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexSTRW
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexSTRX
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

if(Offset >= 0 && Offset <= 4096 && AArch64Base_LDRB_offset_predicate(Offset))
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
bool eliminateFrameIndexSTURB
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
bool eliminateFrameIndexSTURH
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
bool eliminateFrameIndexSTURW
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
bool eliminateFrameIndexSTURX
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
case processornamevalue::LDRB:
{
error = eliminateFrameIndexLDRB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRH:
{
error = eliminateFrameIndexLDRH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRSBW:
{
error = eliminateFrameIndexLDRSBW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRSBX:
{
error = eliminateFrameIndexLDRSBX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRSHW:
{
error = eliminateFrameIndexLDRSHW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRSHX:
{
error = eliminateFrameIndexLDRSHX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRSWX:
{
error = eliminateFrameIndexLDRSWX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRW:
{
error = eliminateFrameIndexLDRW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDRX:
{
error = eliminateFrameIndexLDRX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURB:
{
error = eliminateFrameIndexLDURB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURH:
{
error = eliminateFrameIndexLDURH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURSBW:
{
error = eliminateFrameIndexLDURSBW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURSBX:
{
error = eliminateFrameIndexLDURSBX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURSHW:
{
error = eliminateFrameIndexLDURSHW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURSHX:
{
error = eliminateFrameIndexLDURSHX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURSWX:
{
error = eliminateFrameIndexLDURSWX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURW:
{
error = eliminateFrameIndexLDURW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::LDURX:
{
error = eliminateFrameIndexLDURX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRB:
{
error = eliminateFrameIndexSTRB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRH:
{
error = eliminateFrameIndexSTRH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRW:
{
error = eliminateFrameIndexSTRW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STRX:
{
error = eliminateFrameIndexSTRX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STURB:
{
error = eliminateFrameIndexSTURB(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STURH:
{
error = eliminateFrameIndexSTURH(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STURW:
{
error = eliminateFrameIndexSTURW(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
break;
}
case processornamevalue::STURX:
{
error = eliminateFrameIndexSTURX(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
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
return TFI->hasFP(MF) ? processornamevalue::X29 /* FP */ : processornamevalue::S31 /* SP */;
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
