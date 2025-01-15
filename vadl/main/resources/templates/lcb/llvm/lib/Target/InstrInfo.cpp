#include "[(${namespace})]InstrInfo.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "[(${namespace})]RegisterInfo.h"
#include "llvm/CodeGen/MachineInstrBuilder.h"
#include "llvm/MC/MCInstBuilder.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/Analysis/MemoryLocation.h"
#include "llvm/CodeGen/LiveIntervals.h"
#include "llvm/CodeGen/LiveVariables.h"
#include "llvm/CodeGen/MachineCombinerPattern.h"
#include "llvm/CodeGen/MachineFunctionPass.h"
#include "llvm/CodeGen/MachineInstrBuilder.h"
#include "llvm/CodeGen/MachineRegisterInfo.h"
#include "llvm/CodeGen/MachineTraceMetrics.h"
#include "llvm/CodeGen/RegisterScavenging.h"
#include "llvm/IR/DebugInfoMetadata.h"
#include "llvm/MC/TargetRegistry.h"
#include "Utils/ImmediateUtils.h"
#include <math.h>
#include <iostream>

#define DEBUG_TYPE "[(${namespace})]InstrInfo"
#include "llvm/Support/Debug.h"

using namespace llvm;

#define GET_INSTRINFO_CTOR_DTOR
#include "[(${namespace})]GenInstrInfo.inc"

void [(${namespace})]InstrInfo::anchor() {}

const MCInstrDesc &[(${namespace})]InstrInfo::getBrCond([(${namespace})]CC::CondCode CC) const {
  switch (CC) {
  default:
    llvm_unreachable("Unknown condition code!");
  case [(${namespace})]CC::COND_EQ:
    return get([(${namespace})]::[(${beq.identifier.simpleName()})]);
  case [(${namespace})]CC::COND_NE:
    return get([(${namespace})]::[(${bne.identifier.simpleName()})]);
  case [(${namespace})]CC::COND_LT:
    return get([(${namespace})]::[(${blt.identifier.simpleName()})]);
  case [(${namespace})]CC::COND_GE:
    return get([(${namespace})]::[(${bge.identifier.simpleName()})]);
  case [(${namespace})]CC::COND_LTU:
    return get([(${namespace})]::[(${bltu.identifier.simpleName()})]);
  case [(${namespace})]CC::COND_GEU:
    return get([(${namespace})]::[(${bgeu.identifier.simpleName()})]);
  }
}

static [(${namespace})]CC::CondCode getCondFromBranchOpc(unsigned Opc) {
  switch (Opc) {
  default:
    return [(${namespace})]CC::COND_INVALID;
  case [(${namespace})]::[(${beq.identifier.simpleName()})]:
    return [(${namespace})]CC::COND_EQ;
  case [(${namespace})]::[(${bne.identifier.simpleName()})]:
    return [(${namespace})]CC::COND_NE;
  case [(${namespace})]::[(${blt.identifier.simpleName()})]:
    return [(${namespace})]CC::COND_LT;
  case [(${namespace})]::[(${bge.identifier.simpleName()})]:
    return [(${namespace})]CC::COND_GE;
  case [(${namespace})]::[(${bltu.identifier.simpleName()})]:
    return [(${namespace})]CC::COND_LTU;
  case [(${namespace})]::[(${bgeu.identifier.simpleName()})]:
    return [(${namespace})]CC::COND_GEU;
  }
}

[(${namespace})]CC::CondCode [(${namespace})]CC::getOppositeBranchCondition([(${namespace})]CC::CondCode CC) {
  switch (CC) {
  default:
    llvm_unreachable("Unrecognized conditional branch");
  case [(${namespace})]CC::COND_EQ:
    return [(${namespace})]CC::COND_NE;
  case [(${namespace})]CC::COND_NE:
    return [(${namespace})]CC::COND_EQ;
  case [(${namespace})]CC::COND_LT:
    return [(${namespace})]CC::COND_GE;
  case [(${namespace})]CC::COND_GE:
    return [(${namespace})]CC::COND_LT;
  case [(${namespace})]CC::COND_LTU:
    return [(${namespace})]CC::COND_GEU;
  case [(${namespace})]CC::COND_GEU:
    return [(${namespace})]CC::COND_LTU;
  }
}

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

MachineBasicBlock *[(${namespace})]InstrInfo::getBranchDestBlock(const MachineInstr &MI) const {
  assert(MI.getDesc().isBranch() && "Unexpected opcode!");
  // The branch target is always the last operand.
  int NumOp = MI.getNumExplicitOperands();
  return MI.getOperand(NumOp - 1).getMBB();
}

// The contents of values added to Cond are not examined outside of
// RISCVInstrInfo, giving us flexibility in what to push to it. For RISCV, we
// push BranchOpcode, Reg1, Reg2.
static void parseCondBranch(MachineInstr &LastInst, MachineBasicBlock *&Target,
                            SmallVectorImpl<MachineOperand> &Cond) {
  // Block ends with fall-through condbranch.
  assert(LastInst.getDesc().isConditionalBranch() &&
         "Unknown conditional branch");
  Target = LastInst.getOperand(2).getMBB();
  unsigned CC = getCondFromBranchOpc(LastInst.getOpcode());
  Cond.push_back(MachineOperand::CreateImm(CC));
  Cond.push_back(LastInst.getOperand(0));
  Cond.push_back(LastInst.getOperand(1));
}

bool [(${namespace})]InstrInfo::analyzeBranch(MachineBasicBlock &MBB, MachineBasicBlock *&TBB,
                     MachineBasicBlock *&FBB,
                     SmallVectorImpl<MachineOperand> &Cond,
                     bool AllowModify) const {
  TBB = FBB = nullptr;
  Cond.clear();

  MachineBasicBlock::iterator I = MBB.getLastNonDebugInstr();
  if (I == MBB.end() || !isUnpredicatedTerminator(*I))
      return false;

  // Count the number of terminators and find the first unconditional or
  // indirect branch.
  int NumTerminators = 0;
  MachineBasicBlock::iterator FirstUncondOrIndirectBr = MBB.end();
  for (auto J = I.getReverse(); J != MBB.rend() && isUnpredicatedTerminator(*J);
       J++) {
    NumTerminators++;
    if (J->getDesc().isUnconditionalBranch() ||
        J->getDesc().isIndirectBranch()) {
      FirstUncondOrIndirectBr = J.getReverse();
    }
  }

  if (AllowModify && FirstUncondOrIndirectBr != MBB.end()) {
      while (std::next(FirstUncondOrIndirectBr) != MBB.end()) {
        std::next(FirstUncondOrIndirectBr)->eraseFromParent();
        NumTerminators--;
      }
      I = FirstUncondOrIndirectBr;
  }

  // We can't handle blocks that end in an indirect branch.
  if (I->getDesc().isIndirectBranch())
    return true;

  // We can't handle blocks with more than 2 terminators.
  if (NumTerminators > 2)
    return true;

  // Handle a single unconditional branch.
  if (NumTerminators == 1 && I->getDesc().isUnconditionalBranch()) {
    TBB = getBranchDestBlock(*I);
    return false;
  }

  // Handle a single conditional branch.
  if (NumTerminators == 1 && I->getDesc().isConditionalBranch()) {
    parseCondBranch(*I, TBB, Cond);
    return false;
  }

  // Handle a conditional branch followed by an unconditional branch.
  if (NumTerminators == 2 && std::prev(I)->getDesc().isConditionalBranch() &&
      I->getDesc().isUnconditionalBranch()) {
    parseCondBranch(*std::prev(I), TBB, Cond);
    FBB = getBranchDestBlock(*I);
    return false;
  }

  return true;
}

bool [(${namespace})]InstrInfo::isBranchOffsetInRange(unsigned BranchOp, int64_t BrOffset) const {
  switch (BranchOp) {
    default:
      std::cerr << "Op " << BranchOp << std::endl;
      llvm_unreachable("Unexpected opcode!");
    [# th:each="branch : ${branchInstructions}" ]
    case [(${namespace})]::[(${branch.name})]:
      return isIntN([(${branch.bitWidth})], BrOffset);
    [/]
  }
}

unsigned [(${namespace})]InstrInfo::getInstSizeInBytes(const MachineInstr &MI) const {
  if (MI.isMetaInstruction())
    return 0;

  unsigned Opcode = MI.getOpcode();

  return get(Opcode).getSize();
}

// Inserts a branch into the end of the specific MachineBasicBlock, returning
// the number of instructions inserted.
unsigned [(${namespace})]InstrInfo::insertBranch(
    MachineBasicBlock &MBB, MachineBasicBlock *TBB, MachineBasicBlock *FBB,
    ArrayRef<MachineOperand> Cond, const DebugLoc &DL, int *BytesAdded) const {
  if (BytesAdded)
    *BytesAdded = 0;

  // Shouldn't be a fall through.
  assert(TBB && "insertBranch must not be told to insert a fallthrough");
  assert((Cond.size() == 3 || Cond.size() == 0) &&
         "[(${namespace})] branch conditions have two components!");

  // Unconditional branch.
  if (Cond.empty()) {
    MachineInstr &MI = *BuildMI(&MBB, DL, get([(${namespace})]::[(${jumpInstruction.identifier.simpleName()})])).addMBB(TBB);
    if (BytesAdded)
      *BytesAdded += getInstSizeInBytes(MI);
    return 1;
  }

  // Either a one or two-way conditional branch.
  auto CC = static_cast<[(${namespace})]CC::CondCode>(Cond[0].getImm());
    MachineInstr &CondMI =
        *BuildMI(&MBB, DL, getBrCond(CC)).add(Cond[1]).add(Cond[2]).addMBB(TBB);
  if (BytesAdded)
    *BytesAdded += getInstSizeInBytes(CondMI);

  // One-way conditional branch.
  if (!FBB)
    return 1;

  // Two-way conditional branch.
  MachineInstr &MI = *BuildMI(&MBB, DL, get([(${namespace})]::[(${jumpInstruction.identifier.simpleName()})])).addMBB(FBB);
  if (BytesAdded)
    *BytesAdded += getInstSizeInBytes(MI);
  return 2;
}

unsigned [(${namespace})]InstrInfo::removeBranch(MachineBasicBlock &MBB,
                                      int *BytesRemoved) const {
  if (BytesRemoved)
    *BytesRemoved = 0;
  MachineBasicBlock::iterator I = MBB.getLastNonDebugInstr();
  if (I == MBB.end())
    return 0;

  if (!I->getDesc().isUnconditionalBranch() &&
      !I->getDesc().isConditionalBranch())
    return 0;

  // Remove the branch.
  if (BytesRemoved)
    *BytesRemoved += getInstSizeInBytes(*I);
  I->eraseFromParent();

  I = MBB.end();

  if (I == MBB.begin())
    return 1;
  --I;
  if (!I->getDesc().isConditionalBranch())
    return 1;

  // Remove the branch.
  if (BytesRemoved)
    *BytesRemoved += getInstSizeInBytes(*I);
  I->eraseFromParent();
  return 2;
}
