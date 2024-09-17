#include "[(${namespace})]ExpandPseudo.h"

using namespace llvm;

bool [(${namespace})]ExpandPseudo::runOnMachineFunction(MachineFunction &MF)
{
    TII = static_cast<const [(${namespace})]InstrInfo *>(MF.getSubtarget().getInstrInfo());
    bool Modified = false;
    for (auto &MBB : MF)
    {
        Modified |= expandMBB(MBB);
    }
    return Modified;
}

bool [(${namespace})]ExpandPseudo::expandMBB(MachineBasicBlock &MBB)
{
    bool Modified = false;
    MachineBasicBlock::iterator MBBI = MBB.begin();
    MachineBasicBlock::iterator MBBEnd = MBB.end();
    while (MBBI != MBBEnd)
    {
        MachineBasicBlock::iterator NMBBI = std::next(MBBI);
        Modified |= expandMI(MBB, MBBI, NMBBI);
        MBBI = NMBBI;
    }
    return Modified;
}

////
//
// Machine Instruction Expansion Methods
//

bool [(${namespace})]ExpandPseudo::expandMI(MachineBasicBlock &MBB, MachineBasicBlock::iterator MBBI, MachineBasicBlock::iterator &NextMBBI)
{
    MachineInstr &MI = *MBBI;
    if (requiresExpansion(MI) == false)
    {
        return false; // early return
    }

    unsigned opcode = MI.getOpcode();
    switch (opcode)
    {
    // auto generated cases
    }

    // success and machine basic block was modified
    return true;
}

bool [(${namespace})]ExpandPseudo::requiresExpansion(MachineInstr &MI)
{
    unsigned opcode = MI.getOpcode();
    switch (opcode)
    {
        default : return false;
    }
}

MachineOperand [(${namespace})]ExpandPseudo::copyImmOp(const MachineOperand &MO, unsigned TargetFlag, ImmediateUtils::[(${namespace})]ImmediateKind ImmediateFlag)
{
    //
    // special imm behavior
    //

    if (MO.isImm())
    {
        int64_t value = MO.getImm();
        if (TargetFlag != [(${namespace})]BaseInfo::MO_None) // needs calculations
        {
            // This applies the relocation based on the TargetFlag
            value = [(${namespace})]BaseInfo::applyRelocation(value, TargetFlag);

            // After the relocation the immediate has its "encoded" form and needs to be "decoded" again.
            // This is way the ImmediateFlag is needed to indicate which decoding should be applied.
            value = ImmediateUtils::applyDecoding(value, ImmediateFlag);
        }
        return MachineOperand::CreateImm(value); // early return
    }

    //
    // handle different symbol operand types, etc
    //

    switch (MO.getType())
    {
    default:
        llvm_unreachable("Wrong machine operand type for 'copyImmOp'");
    case MachineOperand::MO_MachineBasicBlock:
        return MachineOperand::CreateMBB(MO.getMBB(), TargetFlag);
    case MachineOperand::MO_GlobalAddress:
        return MachineOperand::CreateGA(MO.getGlobal(), MO.getOffset(), TargetFlag);
    case MachineOperand::MO_ExternalSymbol:
        return MachineOperand::CreateES(MO.getSymbolName(), TargetFlag);
    case MachineOperand::MO_JumpTableIndex:
        return MachineOperand::CreateJTI(MO.getIndex(), TargetFlag);
    case MachineOperand::MO_ConstantPoolIndex:
        return MachineOperand::CreateCPI(MO.getIndex(), MO.getOffset(), TargetFlag);
    case MachineOperand::MO_BlockAddress:
        return MachineOperand::CreateBA(MO.getBlockAddress(), MO.getOffset(), TargetFlag);
    }
}

MachineOperand [(${namespace})]ExpandPseudo::copyRegOp(const MachineOperand &MO, bool isDef, unsigned SubReg)
{
    assert(MO.isReg() && "Wrong machine operand type for 'copyRegOp'");
    // MachineOperand MOCopy = MachineOperand::CreateReg( MO.getReg(), isDef );
    MachineOperand MOCopy = MachineOperand::CreateReg(MO.getReg(), MO.isDef()); // TODO: @chochrainer replace this by passed flag
    if (SubReg != 0)
    {
        MOCopy.setSubReg(SubReg);
    }
    return MOCopy;
}

////
//
// LLVM pass registration
//

// generates the necessary pass setups
char [(${namespace})]ExpandPseudo::ID = 0;
INITIALIZE_PASS([(${namespace})]ExpandPseudo, "«namespace»-expand-pseudo",
                        [(${namespace})]_EXPAND_PSEUDO_NAME, false, false)

// global function is declared in '«namespace».h'
// used to create the pass and add it in the '«namespace»PassConfig.h'
FunctionPass *llvm::create[(${namespace})]ExpandPseudoPass()
{
    return new [(${namespace})]ExpandPseudo();
}