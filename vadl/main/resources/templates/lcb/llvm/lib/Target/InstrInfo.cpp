#include "[(${namespace})]InstrInfo.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "[(${namespace})]RegisterInfo.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include "Utils/ImmediateUtils.h"
#include <math.h>

#define DEBUG_TYPE "[(${namespace})]InstrInfo"
#include "llvm/Support/Debug.h"

using namespace llvm;

#define GET_INSTRINFO_CTOR_DTOR
#include "[(${namespace})]GenInstrInfo.inc"

void [(${namespace})]InstrInfo::anchor() {}

[(${namespace})]InstrInfo::[(${namespace})]InstrInfo( [(${namespace})]Subtarget &STI)
    : [(${namespace})]GenInstrInfo( [(${namespace})]::ADJCALLSTACKDOWN, [(${namespace})]::ADJCALLSTACKUP), STI(STI)
{
}

void [(${namespace})]InstrInfo::copyPhysReg(MachineBasicBlock &MBB, MachineBasicBlock::iterator MBBI, const DebugLoc &DL, MCRegister DestReg, MCRegister SrcReg, bool KillSrc) const
{
  [# th:each="r : ${copyPhysInstructions}" ]
  if ( [(${namespace})]::[(${r.destRegisterFile.identifier.simpleName()})]RegClass.contains( DestReg )
                && [(${namespace})]::[(${r.srcRegisterFile.identifier.simpleName()})]RegClass.contains( SrcReg ) )
    {
        BuildMI( MBB, MBBI, DL, get( [(${namespace})]::[(${r.instruction.identifier.simpleName()})] ) )
            .addReg( DestReg, RegState::Define )
            .addReg( SrcReg, getKillRegState( KillSrc ) )
            ;

        return; // success
    }
  [/]

  llvm_unreachable("Can't copy source to destination register");
}

void [(${namespace})]InstrInfo::storeRegToStackSlot(MachineBasicBlock &MBB, MachineBasicBlock::iterator MBBI, Register SrcReg, bool IsKill, int FrameIndex, const TargetRegisterClass *RC, const TargetRegisterInfo *TRI, Register VReg) const
{
    // TODO: @chochrainer create pseudo spill instructions and use memory operands

    assert(RC != nullptr && "Register Class was null for 'storeRegToStackSlot'");

    DebugLoc DL;
    if (MBBI != MBB.end())
    {
        DL = MBBI->getDebugLoc();
    }

    /*
    «FOR sequence : storeRegToStackSequences»
      «emitStore(sequence)»
    «ENDFOR»
    */

    llvm_unreachable("Can't store this register to stack slot");
}

void [(${namespace})]InstrInfo::loadRegFromStackSlot(MachineBasicBlock &MBB, MachineBasicBlock::iterator MBBI, Register DestReg, int FrameIndex, const TargetRegisterClass *RC, const TargetRegisterInfo *TRI, Register) const
{
    // TODO: @chochrainer create pseudo spill instructions and use memory operands

    assert(RC != nullptr && "Register Class was null for 'loadRegFromStackSlot'");

    DebugLoc DL;
    if (MBBI != MBB.end())
    {
        DL = MBBI->getDebugLoc();
    }

    /*
    «FOR sequence : loadRegFromStackSequences»
      «emitLoad(sequence)»
    «ENDFOR»
    */

    llvm_unreachable("Can't load this register from stack slot");
}

std::vector<int> splitNumber(int number)
{
    std::vector<int> parts;

    /*
    «val additionInstruction = findAdditionImmediateMachineInstruction()»
        // The most ugly hack ever
        // This highly depends of the order of the immediate constraints. It extracts the last one.
            «val encoding = additionInstruction.encoding().fields().stream().filter([a | a.isDynamic()]).reduce([ a, b | b ]).get() »
            «val encodingStart = encoding.ranges().get(0).begin().value() »
            «val encodingEnd = encoding.ranges().get(0).end().value() » int max = pow(2, «encodingEnd.subtract(encodingStart).intValue()») - 1;
    */
    auto max = 0; //TODO remove

    number = abs(number);
    while (number >= max)
    {
        parts.push_back(max);
        number -= max;
    }

    parts.push_back(number);

    return parts;
}

bool [(${namespace})]InstrInfo::adjustReg(MachineBasicBlock &MBB, MachineBasicBlock::iterator MBBI, const DebugLoc &DL, Register DestReg, Register SrcReg, int64_t Val, MachineInstr::MIFlag Flag) const
{
    MachineRegisterInfo &MRI = MBB.getParent()->getRegInfo();

    if (DestReg == SrcReg && Val == 0)
    {
        return false; // success
    }

    int64_t negatedVal = -Val;

    /*
    «FOR sequence : adjRegSequences»
                «emitAdjustRegCase(sequence)»
            «ENDFOR»
    */

    auto parts = splitNumber(Val);

    // First define the destination register
    /*
    BuildMI(MBB, MBBI, DL, get(«processor.simpleName()»::«additionInstruction.simpleName()»))
        .addReg(DestReg, RegState::Define)
        .addReg(SrcReg)
        .addImm(Val >= 0 ? parts.at(0) : parts.at(0) * -1)
        .setMIFlag(Flag);
        */

    // Then add the remaining values
    for (auto v = ++parts.begin(); v != parts.end(); ++v)
    {
        /*
        BuildMI(MBB, MBBI, DL, get(«processor.simpleName()»::«additionInstruction.simpleName()»))
            .addReg(DestReg)
            .addReg(DestReg)
            .addImm(Val >= 0 ? *v : (*v) * -1)
            .setMIFlag(Flag);
        */
    }

    return false; // success
}