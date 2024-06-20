#include "[(${namespace})]RegisterInfo.h"
#include "[(${namespace})]FrameLowering.h"
#include "[(${namespace})]InstrInfo.h"
#include "[(${namespace})]Subtarget.h"
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
    : [(${namespace})]GenRegisterInfo( «emitWithNamespace(returnAddress)» )
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

    markSuperRegs(Reserved, «emitWithNamespace(stackPointer)»); // stack pointer
    «IF hasGlobalPointer»
        markSuperRegs(Reserved, «emitWithNamespace(globalPointer)»); // global pointer
    «ENDIF»
            «IF hasFramePointer»
        markSuperRegs(Reserved, «emitWithNamespace(framePointer)»); // frame pointer
    «ENDIF»

            «IF constantRegisters.size != 0 »
        // constant registers ( e.g. zero register )
                «FOR register : constantRegisters » markSuperRegs(Reserved,  «emitWithNamespace(register)»);
    «ENDFOR»
            «ENDIF»

        assert(checkAllSuperRegsMarked(Reserved));

    return Reserved;
}

«FOR definition : frameIndexEliminations»
            «emitEliminateFrameIndexMethod(definition)»

        «ENDFOR»

                  /**
                   * This method calls its own replacement class for each allowed instruction.
                   * Inside the special instruction the following steps or tries to remove the FI are done.
                   *
                   *     1. try to inline the frame index calculation into the current instruction.
                   *     2. check if we can move the immediate materialization and frame index addition
                   *        into a separate instruction with scratch register. The scratch register must be
                   *        useable with our current instruction.
                   *
                   * TODO: @chochrainer:
                   *
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
    «FOR definition : frameIndexEliminations» case [(${namespace})]::«definition.instruction.simpleName»:
    {
        error = eliminateFrameIndex«definition.instruction.simpleName»(II, SPAdj, FIOperandNum, FrameReg, FrameIndexOffset, RS);
        break;
    }
        «ENDFOR» default:
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
    «IF hasFramePointer» const TargetFrameLowering *TFI = getFrameLowering(MF);
    return TFI->hasFP(MF) ? «emitWithNamespace(framePointer)» /* FP */ : «emitWithNamespace(stackPointer)» /* SP */;
    «ELSE» return «emitWithNamespace(stackPointer)»; // stack pointer
    «ENDIF»
}

const uint32_t * [(${namespace})]RegisterInfo::getCallPreservedMask(const MachineFunction & /*MF*/
                                                                   ,
                                                                   CallingConv::ID /*CC*/
) const
{
    // defined in calling convention tablegen
    return CSR_[(${namespace})]_RegMask;
}

«FOR registerClass : registerClasses»
                     /*static*/ unsigned [(${namespace})]RegisterInfo::«registerClass.simpleName»(unsigned index)
{
    switch (index)
    {
    «FOR entry : registerClass.asMap» case «entry.getKey»:
        return [(${namespace})]::«entry.getValue.simpleName»;
        «ENDFOR» default:
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
«ENDFOR»

    /*static*/ unsigned [(${namespace})]RegisterInfo::registerOpcodeLookup(std::string className, unsigned index)
{
    «FOR registerClass : registerClasses» if (std::string("«registerClass.simpleName»").compare(className) == 0)
    {
        return [(${namespace})]RegisterInfo::«registerClass.simpleName»(index);
    }
    «ENDFOR»

        // class name was not matched

        std::string errMsg;
    std::stringstream errMsgStream;
    errMsgStream << "Unable to find register class with name " << "'" << className << "' !\n";
    errMsg = errMsgStream.str();
    report_fatal_error(errMsg.c_str());
}