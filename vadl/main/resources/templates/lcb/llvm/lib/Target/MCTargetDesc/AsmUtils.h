#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_ASMUTILS_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_ASMUTILS_H

#include "llvm/ADT/StringRef.h"
#include "llvm/MC/MCInstPrinter.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/Support/Casting.h"
#include "llvm/MC/MCParser/MCAsmLexer.h"
#include "MCTargetDesc/[(${namespace})]MCExpr.h"
#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include <string>

namespace llvm
{
    class MCOperandWrapper; // forward declaration

    class AsmUtils
    {
        public:
            static std::string formatAsmTokenKind(AsmToken::TokenKind Kind);
            static std::string formatImm(MCOperandWrapper Op, uint8_t Radix, const MCAsmInfo *MAI);
            static std::string formatImm(MCOperand Op, uint8_t Radix, const MCAsmInfo *MAI);
            static std::string formatImm(int64_t value, uint8_t Radix, const MCAsmInfo *MAI);
            static std::string formatExpr(const MCExpr *Expr, uint8_t Radix, const MCAsmInfo *MAI);
            static std::string FormatModifier(const [(${namespace})]MCExpr::VariantKind VariantKind);
            static std::string FormatModifier(const MCSymbolRefExpr::VariantKind VariantKind);
            static bool MatchRegNo(StringRef Reg, unsigned &RegNo);
            static bool MatchOpcode(StringRef Mnemonic, unsigned &Opcode);
            static bool MatchCustomModifier(StringRef String, [(${namespace})]MCExpr::VariantKind &VariantKind);
            static bool MatchLlvmModifier(StringRef String, MCSymbolRefExpr::VariantKind &VariantKind);
            static bool evaluateConstantImm(const MCExpr *Expr, int64_t &Imm);
            static bool evaluateConstantImm(const MCOperand *MCOp, int64_t &Imm);

            // register helper function
            static std::string getRegisterName( unsigned RegNo );
            /*
            «FOR cls : processor.list( RegisterClass ).filter[ sideEffect == false ]»
                static std::string getRegisterNameFrom«cls.simpleName»ByIndex( unsigned RegIndex );
            «ENDFOR»
            */
    };

    class MCOperandWrapper
    {
        MCOperand MCOp;

        public:
            MCOperandWrapper(const MCOperand MCOp) : MCOp(MCOp)
            {}

            MCOperand getMCOp() const { return MCOp; }

            operator int64_t() const { return unwrapToIntegral(); }

            int64_t unwrapToIntegral() const
            {
                if( MCOp.isReg() )
                {
                    /*
                    switch( MCOp.getReg() )
                    {
                        «FOR hwc : processor.list( HardwareRegisterClass ).filter[ sideEffect == false ]»
                            «FOR reg : hwc.asList»
                                case [(${namespace})]::«reg.simpleName» : return «reg.hardwareEncoding»;
                            «ENDFOR»
                        «ENDFOR»
                        «FOR reg : processor.list( Register ).filter[ sideEffect == false ]»
                            case [(${namespace})]::«reg.simpleName» : return «reg.hardwareEncoding»;
                        «ENDFOR»
                    }
                    */

                    report_fatal_error("Cannot convert register operand to integral value.");
                }
                else
                {
                    int64_t imm;
                    if( AsmUtils::evaluateConstantImm( &MCOp, imm ) )
                    {
                        return imm;
                    }
                }

                report_fatal_error("Cannot convert operand to integral value.");
            }
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_ASMUTILS_H