#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "MCTargetDesc/[(${namespace})]InstPrinter.h"
#include "Utils/ImmediateUtils.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCSymbol.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Debug.h"
#include "llvm/MC/MCSymbol.h"
#include <string>
#include <iostream>
#include <sstream>
#include <bitset>
#include <iomanip>

#define DEBUG_TYPE "[(${namespace})]InstPrinter"

using namespace llvm;

#define GET_INSTRUCTION_NAME
#include "[(${namespace})]GenAsmWriter.inc"
#undef GET_INSTRUCTION_NAME

void [(${namespace})]InstPrinter::anchor() {}

void [(${namespace})]InstPrinter::printRegName
    ( raw_ostream &O
    , MCRegister RegNo
    ) const
{
    O << AsmUtils::getRegisterName( RegNo );
}

void [(${namespace})]InstPrinter::printInst
    ( const MCInst *MI
    , uint64_t Address
    , StringRef Annot
    , const MCSubtargetInfo &STI
    , raw_ostream &O
    )
{
    O << "\t" << instToString(MI, Address);
    printAnnotation(O, Annot);
}

template <size_t N>
int64_t signExtendBitset(const std::bitset<N>& bits) {
    int64_t value = static_cast<int64_t>(bits.to_ulong());
    if (bits[N - 1]) {
        value |= ~((1 << N) - 1);
    }
    return value;
}

[#th:block th:each="register : ${systemRegisters}" ]
void [(${namespace})]InstPrinter::print[(${register.name})]SystemRegister
            ( const MCInst *MI
            , unsigned OpNo
            , raw_ostream &O
            )
{
    unsigned Imm = MI->getOperand( OpNo ).getImm();
    std::stringstream ss;
    ss << Imm;
    std::string s = ss.str();
    O << s;
}
[/th:block]

std::string [(${namespace})]InstPrinter::instToString(const MCInst *MI, uint64_t Address) const
{
    switch ( MI->getOpcode() )
    {
        [#th:block th:each="instruction : ${instructions}" ]
            case [(${namespace})]::[(${instruction.name})]:
            {
                [(${instruction.code.value})]
                break;
            }
        [/th:block]
    default:
        return std::string("unknown instruction");
    }
}