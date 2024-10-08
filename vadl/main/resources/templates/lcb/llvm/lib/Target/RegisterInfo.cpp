#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]FrameLowering.h"
#include "[(${namespace})]InstrInfo.h"
#include "[(${namespace})]SubTarget.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "Utils/ImmediateUtils.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
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

#define DEBUG_TYPE "[(${namespace})]RegisterInfo"

using namespace llvm;

#define GET_REGINFO_TARGET_DESC
#include "[(${namespace})]GenRegisterInfo.inc"

void [(${namespace})]RegisterInfo::anchor() {}

[(${namespace})]RegisterInfo::[(${namespace})]RegisterInfo()
    : [(${namespace})]GenRegisterInfo( [(${namespace})]::[(${returnAddress.render()})] )
{
}

const uint16_t * [(${namespace})]RegisterInfo::getCalleeSavedRegs(const MachineFunction * /*MF*/
) const
{
    // defined in calling convention tablegen
    return CSR_[(${namespace})]_SaveList;
}

BitVector [(${namespace})]RegisterInfo::getReservedRegs(const MachineFunction &MF) const
{
    BitVector Reserved(getNumRegs());

    markSuperRegs(Reserved, [(${namespace})]::[(${framePointer.render()})]); // frame pointer
    markSuperRegs(Reserved, [(${namespace})]::[(${stackPointer.render()})]); // stack pointer
    markSuperRegs(Reserved, [(${namespace})]::[(${globalPointer.render()})]); // global pointer

    [# th:each="constraint : ${constraints}" ]
    markSuperRegs(Reserved,  [(${namespace})]::[(${constraint.registerFile})][(${constraint.index})]);
    [/]

    assert(checkAllSuperRegsMarked(Reserved));

    return Reserved;
}

[# th:each="fe : ${frameIndexEliminations}" ]
bool eliminateFrameIndex[(${fe.instruction.identifier.simpleName()})]
    ( MachineBasicBlock::iterator II
    , int SPAdj
    , unsigned FIOperandNum
    , unsigned FrameReg
    , StackOffset FrameIndexOffset
    , RegScavenger *RS
    )
{
    assert(  MI.getOperand(FIOperandNum).isFI() && "Frame Index operand position does not match expected position!" );
    assert(  MI.getOperand(FIOperandNum + [(${fe.machineInstructionIndices.relativeDistance})]).isImm() && "Immediate operand position does not match expected position!" );

    MachineInstr &MI = *II;
    MachineOperand &FIOp = MI.getOperand(FIOperandNum);
    MachineOperand &ImmOp = MI.getOperand(FIOperandNum + [(${fe.machineInstructionIndices.relativeDistance})]);

    int Offset = FrameIndexOffset.getFixed() + ImmOp.getImm();

    //
    // try to inline the offset into the instruction
    //

    if([(${fe.predicateMethodName})](Offset))
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
    const [(${namespace})]InstrInfo *TII = MF->getSubtarget<[(${namespace})]Subtarget>().getInstrInfo();

    //
    // try to generate a scratch register and adjust frame register with given offset
    //

    Register ScratchReg = MRI.createVirtualRegister(&[(${namespace})]::[(${fe.registerFile.identifier.simpleName()})]RegClass);
    if(TII->adjustReg(MBB, II, DL, ScratchReg, FrameReg, Offset) == false) // MachineInstr::MIFlag Flag
    {
        // the scratch register can properly be manipulated and used as address register.
        FIOp.ChangeToRegister( ScratchReg, false /*isDef*/, false /*isImpl*/, true /*isKill*/ );
        ImmOp.setImm( 0 );
        return false; // success
    }

    return true; // failure
}
[/]

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
bool [(${namespace})]RegisterInfo::eliminateFrameIndex(MachineBasicBlock::iterator II, int SPAdj, unsigned FIOperandNum, RegScavenger *RS) const
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
        [# th:each="fe : ${frameIndexEliminations}" ]
        case [(${namespace})]::[(${fe.instruction.identifier.simpleName()})]:
        {
          error = eliminateFrameIndex[(${fe.instruction.identifier.simpleName()})](II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
          break;
        }
        [/]
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

Register [(${namespace})]RegisterInfo::getFrameRegister(const MachineFunction &MF) const
{
    const TargetFrameLowering *TFI = getFrameLowering(MF);
    return TFI->hasFP(MF) ? [(${namespace})]::[(${framePointer.render()})] /* FP */ : [(${namespace})]::[(${stackPointer.render()})] /* SP */;
}

const uint32_t * [(${namespace})]RegisterInfo::getCallPreservedMask(const MachineFunction & /*MF*/
                                                                   , CallingConv::ID /*CC*/
) const
{
    // defined in calling convention tablegen
    return CSR_[(${namespace})]_RegMask;
}

[# th:each="registerClass : ${registerClasses}" ]
/*static*/ unsigned [(${namespace})]RegisterInfo::[(${registerClass.registerFile.identifier.simpleName()})](unsigned index)
{
  switch (index)
  {
  [# th:each="register : ${registerClass.registers}" ]
    case [(${register.index})]:
        return [(${namespace})]::[(${register.name})];
  [/]
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
[/]