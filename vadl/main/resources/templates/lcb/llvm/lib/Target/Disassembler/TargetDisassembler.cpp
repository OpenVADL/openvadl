#include "[(${namespace})]Disassembler.h"
#include <iostream>
#include "Utils/ImmediateUtils.h"

#define DEBUG_TYPE "disassembler"

using namespace llvm;

[(${namespace})]Disassembler::[(${namespace})]Disassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian) : MCDisassembler(STI, Ctx), IsBigEndian(isBigEndian)
{
}

/* == Register Classes == */
[#th:block th:each="registerClass : ${registerClasses}" ]
static const unsigned [(${registerClass.registerFile.identifier.simpleName()})]DecoderTable[] = {
  [#th:block th:each="register, iterStat : ${registerClass.registers}" ]
    [(${namespace})]::[(${register.name})][#th:block th:if="${!iterStat.last}"],[/th:block]
  [/th:block]
};
[/th:block]

/* == Immediate Decoding == */
[#th:block th:each="immediate : ${immediates}" ]
DecodeStatus decode[(${immediate.simpleName})](MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
{
    Imm = Imm & [(${immediate.mask})];
    Imm = [(${immediate.decodeMethodName})];
    Inst.addOperand(MCOperand::createImm(Imm));
    return MCDisassembler::Success;
}
[/th:block]

[#th:block th:each="registerClass : ${registerClasses}" ]
static DecodeStatus Decode[(${registerClass.registerFile.identifier.simpleName()})]RegisterClass
    ( MCInst &Inst
    , uint64_t RegNo
    , uint64_t Address
    , const void *Decoder
    )
{
    // check if register number is in range
    if( RegNo >= [(${registerClass.registers.size()})])
        return MCDisassembler::Fail;

    // access custom generated decoder table in register info
    Register reg = [(${registerClass.registerFile.identifier.simpleName()})]DecoderTableName[RegNo];

    // check if decoded register is valid
    if( reg == [(${namespace})]::NoRegister )
        return MCDisassembler::Fail;

    Inst.addOperand( MCOperand::createReg(reg) );
    return MCDisassembler::Success;
}
[/th:block]

#include "[(${namespace})]GenDisassemblerTables.inc"

    DecodeStatus [(${namespace})]Disassembler::getInstruction(MCInst &MI, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CS) const
{
    if (Bytes.size() < [(${instructionSize / 8})])
    {
        Size = 0;
        return MCDisassembler::Fail;
    }

    uint[(${instructionSize / 8})]_t Instr;
    [#th:block th:if="${instructionSize <= 8}"]
        if (IsBigEndian)
        {
            Instr = support::endian::read<uint8_t>(Bytes.data(), support::big);
        }
        else
        {
            Instr = support::endian::read<uint8_t>(Bytes.data(), support::little);
        }
    [/th:block]
    [#th:block th:if="${instructionSize > 8}"]
        if (IsBigEndian)
        {
            Instr = support::endian::read[(${instructionSize})]be(Bytes.data());
        }
        else
        {
            Instr = support::endian::read[(${instructionSize})]le(Bytes.data());
        }
    [/th:block]

    auto Result = decodeInstruction(DecoderTable[(${instructionSize})], MI, Instr, Address, this, STI);
    Size = [(${instructionSize / 8})];
    return Result;
}

static MCDisassembler *create[(${namespace})]Disassembler(const Target &T, const MCSubtargetInfo &STI, MCContext &Ctx)
{
    return new [(${namespace})]Disassembler(STI, Ctx, [(${namespace})]BaseInfo::IsBigEndian());
}

extern "C" void LLVMInitialize[(${namespace})]Disassembler()
{
    // Register Target Disassembler
    TargetRegistry::RegisterMCDisassembler(getThe[(${namespace})]Target(), create[(${namespace})]Disassembler);
}