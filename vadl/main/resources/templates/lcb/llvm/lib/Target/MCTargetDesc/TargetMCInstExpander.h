#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTEXPANDER
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTEXPANDER

#include <vector>
#include <cstdint>

namespace llvm
{
    class MCExpr;
    class MCInst;
    class MCOperand;
    class MCContext;

    class [(${namespace})]MCInstExpander
    {
    public:
        [(${namespace})]MCInstExpander(class MCContext & Ctx);
        bool needsExpansion(const MCInst &MCI) const;
        bool isExpandable(const MCInst &MCI) const;
        bool expand(const MCInst &MCI, std::vector<MCInst> &expansion) const;

    private:
        MCContext & Ctx;

        const MCExpr *MCOperandToMCExpr(const MCOperand &MCO) const;
        const int64_t MCOperandToInt64(const MCOperand &MCO) const;

        //
        // instruction expansion method
        //

        «IF expandableInstructions.size > 0»
                        «FOR instruction : expandableInstructions»
                                                std::vector<MCInst> «emitExpandMethodName(instruction)»(const MCInst &instruction) const;
        «ENDFOR»
                    «ENDIF»

            //
            // sequence expansion method
            //

                    «FOR instruction : abiInstructions»
                        «cppEmitter.emitMethodDeclaration(instruction.MCInstExpanderMethod)»
                    «ENDFOR»
    };
}

#endif