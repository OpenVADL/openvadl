#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTEXPANDER
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]MCINSTEXPANDER

#include <vector>
#include <cstdint>
#include <functional>

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
        bool expand(const MCInst &MCI, std::function<void(const MCInst &)> callback ) const;

    private:
        MCContext & Ctx;

        const MCExpr *MCOperandToMCExpr(const MCOperand &MCO) const;
        const int64_t MCOperandToInt64(const MCOperand &MCO) const;

        //
        // instruction expansion method
        //

        [# th:each="instructionHeaders : ${compilerInstructionHeaders}" ]
        std::vector<MCInst> [(${instructionHeaders})]( const MCInst& instruction, std::function<void(const MCInst &)> callback ) const;
        [/]
    };
}

#endif