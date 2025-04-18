#include "[(${namespace})]MCInstExpander.h"

#include "MCTargetDesc/[(${namespace})]MCTargetDesc.h"
#include "Utils/ImmediateUtils.h"

#include "MCTargetDesc/[(${namespace})]MCExpr.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCSymbol.h"

#define DEBUG_TYPE "[(${namespace})]MCInstExpander"

using namespace llvm;

[(${namespace})]MCInstExpander::[(${namespace})]MCInstExpander(class MCContext &Ctx)
    : Ctx(Ctx) {}

bool [(${namespace})]MCInstExpander::needsExpansion(const MCInst &MCI) const
{
    auto opcode = MCI.getOpcode();
    switch (opcode)
    {
    // instructions
    [# th:each="instruction : ${compilerInstructions}" ]
    case [(${namespace})]::[(${instruction.compilerInstruction.name})]:
    [/]
    {
        return true;
    }
    default:
    {
        return false;
    }
    }
    return false; // unreachable
}

bool [(${namespace})]MCInstExpander::isExpandable(const MCInst &MCI) const
{
    auto opcode = MCI.getOpcode();
    switch (opcode)
    {
    // instructions
    [# th:each="instruction : ${compilerInstructions}" ]
        case [(${namespace})]::[(${instruction.compilerInstruction.name})]:
    [/]
    {
        return true;
    }
    default:
    {
        return false;
    }
    }
    return false; // unreachable
}

bool [(${namespace})]MCInstExpander::expand(const MCInst &MCI,
  std::function<void(const MCInst &)> callback,
  std::function<void(MCSymbol*)> callbackSymbol  ) const
{
    auto opcode = MCI.getOpcode();
    switch (opcode)
    {
        //
        // instructions
        //

    [# th:each="instruction : ${compilerInstructions}" ]
      case [(${namespace})]::[(${instruction.compilerInstruction.name})]:
      {
        [(${instruction.header})](MCI, callback, callbackSymbol );
        return true;
      }
    [/]
      default:
        {
            return false;
        }
    }
    return false; // unreachable
}

const MCExpr *[(${namespace})]MCInstExpander::MCOperandToMCExpr(const MCOperand &MCO) const
{
    if (MCO.isImm())
    {
        return MCConstantExpr::create(MCO.getImm(), Ctx);
    }

    if (MCO.isExpr())
    {
        return MCO.getExpr();
    }

    llvm_unreachable("<unsupported mc operand type>");
}

const int64_t [(${namespace})]MCInstExpander::MCOperandToInt64(const MCOperand &MCO) const
{
    if (MCO.isImm())
    {
        return MCO.getImm();
    }

    if (MCO.isExpr())
    {
        int64_t mcExprResult;
        const MCExpr *mcExpr = MCO.getExpr();
        if (mcExpr->evaluateAsAbsolute(mcExprResult))
        {
            return mcExprResult;
        }
    }

    llvm_unreachable("<unsupported operand type or value>");
}

[# th:each="instruction : ${compilerInstructions}" ]

[(${instruction.code})]

[/]