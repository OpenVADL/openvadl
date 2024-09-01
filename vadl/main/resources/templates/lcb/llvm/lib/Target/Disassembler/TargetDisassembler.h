#ifndef LLVM_LIB_TARGET_[(${namespace})]_DISASSEMBLER_[(${namespace})]DISASSEMBLER_H
#define LLVM_LIB_TARGET_[(${namespace})]_DISASSEMBLER_[(${namespace})]DISASSEMBLER_H

#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "TargetInfo/[(${namespace})]TargetInfo.h"
#include "Utils/[(${namespace})]BaseInfo.h"
#include "llvm/CodeGen/Register.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCDisassembler/MCDisassembler.h"
#include "llvm/MC/MCDecoderOps.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCRegisterInfo.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include "llvm/Support/Endian.h"
#include "llvm/MC/TargetRegistry.h"

using namespace llvm;

typedef MCDisassembler::DecodeStatus DecodeStatus;

namespace llvm
{
    class [(${namespace})]Disassembler : public MCDisassembler
    {
    public:
        [(${namespace})]Disassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian);

        DecodeStatus getInstruction(MCInst &Instr, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CStream) const override;

    protected:
        bool IsBigEndian;
    };
} // end llvm namespace

#endif