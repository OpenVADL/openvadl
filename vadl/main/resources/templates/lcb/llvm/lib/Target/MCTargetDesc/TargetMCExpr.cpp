#include "Utils/[(${namespace})]BaseInfo.h"
#include "Utils/ImmediateUtils.h"
#include "[(${namespace})]MCExpr.h"
#include "[(${namespace})].h"
#include "[(${namespace})]FixupKinds.h"
#include "AsmUtils.h"
#include "llvm/MC/MCAssembler.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCStreamer.h"
#include "llvm/MC/MCValue.h"
#include "llvm/Support/ErrorHandling.h"
#include <sstream>
#include <iostream>
#include <format>
#include <vector>

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

template<typename T>
std::string to_string_any(const T& value) {
    std::ostringstream oss;
    oss << value;
    return oss.str();
}

// Remove when llvm supports C++ 20.
template<typename... Args>
std::string custom_format(const std::string& fmt, Args&&... args) {
    // Convert all arguments to strings
    std::vector<std::string> values{ to_string_any(std::forward<Args>(args))... };

    std::string result;
    size_t argIndex = 0;

    for (size_t i = 0; i < fmt.size(); ++i) {
        if (fmt[i] == '{' && i + 1 < fmt.size() && fmt[i + 1] == '}' && argIndex < values.size()) {
            result += values[argIndex++];
            ++i; // skip the '}'
        } else {
            result += fmt[i];
        }
    }
    return result;
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

    if (Kind == [(${namespace})]MCExpr::VariantKind::[(${pltVariantKindName})])
    {
        std::string suffix = "@plt";
        return subexpr + suffix;
    }

    std::string fmt = AsmUtils::FormatModifierString(getKind());
    std::string mod = AsmUtils::FormatModifier(getKind());
    std::string result = custom_format(fmt, mod, subexpr);
    return result;
}

bool [(${namespace})]MCExpr::evaluateAsRelocatableImpl(MCValue &Res, const MCAssembler *Layout, const MCFixup *Fixup) const
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

    [# th:each="bi : ${mappingVariantKindsIntoBaseInfos}" ]
      if(Kind == [(${bi.variantKind.value})])
      {
        resultValue = [(${namespace})]BaseInfo::[(${bi.functionName})](resultValue);
      }
    [/]

    [# th:each="decodeMapping : ${decodeMappings}" ]
      if(Kind == [(${decodeMapping.variantKind})])
      {
        // FIXME: invocation when multiple field refs in field access function is incorrect.
        resultValue = [(${decodeMapping.decodeFunction})]([(${decodeMapping.paramString})]);
      }
    [/]

    return resultValue;
}