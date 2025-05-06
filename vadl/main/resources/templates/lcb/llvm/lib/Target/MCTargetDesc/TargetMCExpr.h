#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCEXPR_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCEXPR_H

#include "llvm/MC/MCExpr.h"

namespace llvm
{
    class StringRef;

    class [(${namespace})]MCExpr : public MCTargetExpr
    {
    public:
        enum VariantKind
        {
            [# th:each="vk, iterStat : ${variantKinds}" ]
            [#th:block th:if="${!iterStat.first}"],[/th:block] [(${vk.value})]
            [/]
        };

    private:
        const MCExpr *Expr;
        const VariantKind Kind;

        int64_t evaluateAsInt64(int64_t Value) const;

        explicit [(${namespace})]MCExpr(const MCExpr *Expr, VariantKind Kind)
            : Expr(Expr), Kind(Kind) {}

    public:
        static const [(${namespace})]MCExpr *create(const MCExpr *Expr, VariantKind Kind, MCContext &Ctx);
        VariantKind getKind() const { return Kind; }
        const MCExpr *getSubExpr() const { return Expr; }

        std::string format(uint8_t Radix, const MCAsmInfo *MAI) const;
        bool evaluateAsRelocatableImpl(MCValue & Res, const MCAsmLayout *Layout, const MCFixup *Fixup) const override;
        void visitUsedExpr(MCStreamer & Streamer) const override;

        void fixELFSymbolsInTLSFixups(MCAssembler &) const override
        {
            return;
        }

        MCFragment *findAssociatedFragment() const override
        {
            return getSubExpr()->findAssociatedFragment();
        }

        bool isInternalImmExpr() const;

        bool evaluateAsConstant(int64_t & Res) const;

        static bool classof(const MCExpr *E)
        {
            return E->getKind() == MCExpr::Target;
        }

        static bool classof(const [(${namespace})]MCExpr *) { return true; }
        static VariantKind getVariantKindForName(StringRef name);
        static StringRef getVariantKindName(VariantKind Kind);
    };

} // end namespace llvm.

#endif // LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCEXPR_H