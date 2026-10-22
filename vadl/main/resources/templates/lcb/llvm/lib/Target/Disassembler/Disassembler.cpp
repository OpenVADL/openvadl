#include "[(${namespace})]Disassembler.h"
#include <iostream>
#include "Utils/ImmediateUtils.h"
#include "MCTargetDesc/AsmUtils.h"

#define DEBUG_TYPE "disassembler"

using namespace llvm;

[(${namespace})]Disassembler::[(${namespace})]Disassembler
    ( const MCSubtargetInfo &STI
    , MCContext &Ctx
    , bool isBigEndian
    ) : MCDisassembler( STI, Ctx )
      , IsBigEndian( isBigEndian )
{}

/* == Register Classes == */
[# th:each="regClass : ${decodeRegisterClasses}"]
static DecodeStatus Decode[(${regClass.registerFile.name})]RegisterClass
    ( MCInst &Inst
    , uint64_t RegNo
    , uint64_t Address
    , const void *Decoder
    )
{
    Register reg = AsmUtils::getRegNoFrom[(${regClass.registerFile.name})]ByIndex(RegNo);

    // check if decoded register is valid
    if( reg == [(${namespace})]::NoRegister )
        return MCDisassembler::Fail;

    Inst.addOperand( MCOperand::createReg(reg) );
    return MCDisassembler::Success;
}
[/]

/* == Immediate Decoding == */
[# th:each="immediate : ${decodeImmediates}"]
DecodeStatus [(${immediate.decodeWrapper})](MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder) {
    Imm = [(${immediate.decodeFunction})](Imm);
    Inst.addOperand(MCOperand::createImm(Imm));
    return MCDisassembler::Success;
}
[/]


#include "[(${namespace})]GenDisassemblerTables.inc"

// Assemble NumBytes bytes into an integer, honoring target endianness.
static uint64_t readInstruction(ArrayRef<uint8_t> Bytes, unsigned NumBytes, bool IsBigEndian)
{
    uint64_t Value = 0;
    for (unsigned I = 0; I < NumBytes; ++I) {
        unsigned Idx = IsBigEndian ? I : NumBytes - 1 - I;
        Value = (Value << 8) | Bytes[Idx];
    }
    return Value;
}

DecodeStatus [(${namespace})]Disassembler::getInstruction
    ( MCInst &MI
    , uint64_t &Size
    , ArrayRef<uint8_t> Bytes
    , uint64_t Address
    , raw_ostream &CS
    ) const
{
    // Try decoding in ascending order of instruction sizes.
    [# th:each="size : ${instructionSizes}"]
    if (Bytes.size() >= [(${size.bytes})]) {
        uint64_t Instr = readInstruction(Bytes, [(${size.bytes})], IsBigEndian);
        MI.clear();
        if (decodeInstruction(DecoderTable[(${size.bits})], MI, Instr, Address, this, STI)
              != MCDisassembler::Fail) {
            Size = [(${size.bytes})];
            return MCDisassembler::Success;
        }
    }
    [/]

    // Nothing matched, skip smallest known instruction size.
    MI.clear();
    Size = Bytes.size() < [(${minInstructionSizeInBytes})] ? 0 : [(${minInstructionSizeInBytes})];
    return MCDisassembler::Fail;
}


static MCDisassembler *create[(${namespace})]Disassembler
    ( const Target &T
    , const MCSubtargetInfo &STI
    , MCContext &Ctx
    )
{
    return new [(${namespace})]Disassembler(STI, Ctx, [(${namespace})]BaseInfo::IsBigEndian() );
}

extern "C" void LLVMInitialize[(${namespace})]Disassembler()
{
    // Register Target Disassembler
    TargetRegistry::RegisterMCDisassembler
        ( getThe[(${namespace})]Target()
        , create[(${namespace})]Disassembler
        );
}