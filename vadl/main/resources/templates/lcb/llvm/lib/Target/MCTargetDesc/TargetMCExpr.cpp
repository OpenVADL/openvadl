#include "Utils/[(${namespace})]BaseInfo.h"
#include "Utils/ImmediateUtils.h"
#include "[(${namespace})]MCExpr.h"
#include "[(${namespace})].h"
#include "[(${namespace})]FixupKinds.h"
#include "AsmUtils.h"
#include "llvm/MC/MCAsmLayout.h"
#include "llvm/MC/MCAssembler.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCStreamer.h"
#include "llvm/MC/MCValue.h"
#include "llvm/Support/ErrorHandling.h"

using namespace llvm;

#define DEBUG_TYPE "[(${namespace})]MCExpr"

const [(${namespace})]MCExpr *[(${namespace})]MCExpr::create(const MCExpr *Expr, VariantKind Kind, MCContext &Ctx)
{
    return new (Ctx) [(${namespace})]MCExpr(Expr, Kind);
}

/*
 * This method is used to print an expression.
 * Special cases can be made here, e.g. adding '@plt' etc
 */
void [(${namespace})]MCExpr::printImpl(raw_ostream &OS, const MCAsmInfo *MAI) const
{
    OS << format(10, MAI);
}

std::string [(${namespace})]MCExpr::format(uint8_t Radix, const MCAsmInfo *MAI) const
{
    bool HasVariant = (Kind != VK_None);
    int64_t Res = 0;

    if (evaluateAsConstant(Res))
    {
        return AsmUtils::formatImm(Res, Radix, MAI);
    }

    std::string subexpr = AsmUtils::formatExpr(Expr, Radix, MAI);
    if (HasVariant == false)
    {
        return subexpr;
    }

    // TODO: @tschwarzinger @chochrainer find a better solution to deal with
    //       format field --> immediate --> ASM Immediate etc.
    //
    //       Currently these Immediate Variants are introduced in the pseudo
    //       instruction expansion and should be ignored when printing symbols.
    if (isInternalImmExpr())
    {
        return subexpr;
    }

    std::string result = "";
    result += "%";
    result += AsmUtils::FormatModifier(getKind());
    result += "(";
    result += subexpr;
    result += ')';
    return result;
}

bool [(${namespace})]MCExpr::evaluateAsRelocatableImpl(MCValue &Res, const MCAsmLayout *Layout, const MCFixup *Fixup) const
{
    if (!getSubExpr()->evaluateAsRelocatable(Res, Layout, Fixup))
    {
        return false;
    }

    // special handling if Symbol A and Symbol B is set
    if (Res.getSymA() && Res.getSymB())
    {
        // do not allow multiple symbols in MCValue
        return false;
    }

    // one symbol field or a constant is set
    return true;
}

void [(${namespace})]MCExpr::visitUsedExpr(MCStreamer &Streamer) const
{
    Streamer.visitUsedExpr(*getSubExpr());
}

[(${namespace})]MCExpr::VariantKind [(${namespace})]MCExpr::getVariantKindForName(StringRef name)
{
    return StringSwitch<[(${namespace})]MCExpr::VariantKind>(name)
    [# th:each="vk : ${variantKinds}" ]
          .Case("[(${vk.human})]", [(${vk.value})])
    [/]
          .Default(VK_Invalid);
}

StringRef [(${namespace})]MCExpr::getVariantKindName(VariantKind Kind)
{
    switch (Kind)
    {
    [# th:each="vk : ${variantKinds}" ]
      case [(${vk.value})]:
      return "[(${vk.human})]";
    [/]
    default : llvm_unreachable("Invalid symbol kind");
    }
}

bool [(${namespace})]MCExpr::isInternalImmExpr() const
{
    switch(Kind)
    {
    [# th:each="imm : ${immediates}" ]
      case [(${imm})]:
        return true;
    [/]
      default:
        return false;
    }
}

bool [(${namespace})]MCExpr::evaluateAsConstant(int64_t &Res) const
{
    int64_t result;
    MCValue Value;

    if (!getSubExpr()->evaluateAsRelocatable(Value, nullptr, nullptr))
    {
        return false;
    }

    if (!Value.isAbsolute())
    {
        return false;
    }
    result = Value.getConstant();

    auto possible[(${namespace})]MCExpr = dyn_cast<[(${namespace})]MCExpr>(getSubExpr());
    if (possible[(${namespace})]MCExpr != nullptr)
    {
        possible[(${namespace})]MCExpr->evaluateAsConstant(result);
    }

    Res = evaluateAsInt64(result);
    return true;
}

int64_t [(${namespace})]MCExpr::evaluateAsInt64(int64_t Value) const
{
    int64_t resultValue = Value;

    /*
    [# th:each="bi : ${baseInfos}" ]
      if(Kind == baseInfos' variant kind)
      {
        resultValue = [(${namespace})]BaseInfo::[(${bi.functionName})](resultValue);
      }
    [/]
    */

    return resultValue;
}