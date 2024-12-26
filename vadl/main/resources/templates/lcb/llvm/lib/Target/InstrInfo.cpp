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
            .addImm( 0 )
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

    [# th:each="r : ${storeStackSlotInstructions}" ]
      if ( [(${namespace})]::[(${r.destRegisterFile.identifier.simpleName()})]RegClass.hasSubClassEq(RC) )
      {
          BuildMI( MBB, MBBI, DL, get( [(${namespace})]::[(${r.instruction.identifier.simpleName()})] ) )
              .addFrameIndex( FrameIndex )
              .addReg( SrcReg, getKillRegState( IsKill ) )
              .addImm( 0 )
              ;

          return; // success
      }
    [/]

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

    [# th:each="r : ${loadStackSlotInstructions}" ]
    if ( [(${namespace})]::[(${r.destRegisterFile.identifier.simpleName()})]RegClass.hasSubClassEq(RC) )
    {
        BuildMI( MBB, MBBI, DL, get( [(${namespace})]::[(${r.instruction.identifier.simpleName()})] ) )
          .addReg( DestReg, RegState::Define )
          .addFrameIndex( FrameIndex )
          .addImm( 0 )
          ;

        return; // success
    }
    [/]

    llvm_unreachable("Can't load this register from stack slot");
}

std::vector<int> splitNumber(int number)
{
    std::vector<int> parts;
    auto max = pow(2, [(${additionImmSize})] - 1) - 1;

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

    [# th:each="r : ${adjustCases}" ]
    if ( [(${r.predicate.lower()})]( Val ) /* check if immediate fits */
           && ( (SrcReg.isVirtual() && [(${namespace})]::[(${r.srcRegisterFile.identifier.simpleName()})]RegClass.hasSubClassEq(MRI.getRegClass(SrcReg)))
           && ( (DestReg.isVirtual() && [(${namespace})]::[(${r.destRegisterFile.identifier.simpleName()})]RegClass.hasSubClassEq(MRI.getRegClass(DestReg)))
           || (DestReg.isPhysical() && [(${namespace})]::[(${r.destRegisterFile.identifier.simpleName()})]RegClass.contains(DestReg))
           )
            /* check if destination register fits */
           )
       {
           BuildMI( MBB, MBBI, DL, get( [(${namespace})]::[(${r.instruction.identifier.simpleName()})] ) )
               .addReg( DestReg, RegState::Define )
               .addImm( Val )
               .setMIFlag(Flag)
               ;

           return false; // success
       }
    [/]

    auto parts = splitNumber(Val);

    // First define the destination register
    BuildMI(MBB, MBBI, DL, get([(${namespace})]::[(${additionImmInstruction.identifier.simpleName()})]))
        .addReg(DestReg, RegState::Define)
        .addReg(SrcReg)
        .addImm(Val >= 0 ? parts.at(0) : parts.at(0) * -1)
        .setMIFlag(Flag);

    // Then add the remaining values
    for (auto v = ++parts.begin(); v != parts.end(); ++v)
    {
      BuildMI(MBB, MBBI, DL, get([(${namespace})]::[(${additionImmInstruction.identifier.simpleName()})]))
        .addReg(DestReg)
        .addReg(DestReg)
        .addImm(Val >= 0 ? *v : (*v) * -1)
        .setMIFlag(Flag);
    }

    return false; // success
}