package vadl.test.lcb.template.MCTargetDesc;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitMCInstExpanderCppFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitMCInstExpanderCppFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitMCInstExpanderCppFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #include "rv64imMCInstExpander.h"
                
        #include "MCTargetDesc/rv64imMCTargetDesc.h"
        #include "Utils/ImmediateUtils.h"
                
        #include "MCTargetDesc/rv64imMCExpr.h"
        #include "llvm/MC/MCInst.h"
        #include "llvm/MC/MCExpr.h"
        #include "llvm/MC/MCContext.h"
                
        #define DEBUG_TYPE "rv64imMCInstExpander"
                
        using namespace llvm;
                
        rv64imMCInstExpander::rv64imMCInstExpander(class MCContext &Ctx)
            : Ctx(Ctx) {}
                
        bool rv64imMCInstExpander::needsExpansion(const MCInst &MCI) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
            // instructions
            /*
           \s
            case rv64im::CALL:
            case rv64im::TAIL:
            case rv64im::RET:
            case rv64im::J:
            case rv64im::NOP:
            case rv64im::MOV:
            case rv64im::NOT:
            case rv64im::NEG:
            case rv64im::SNEZ:
            case rv64im::SLTZ:
            case rv64im::SGTZ:
            case rv64im::BEQZ:
            case rv64im::BNEZ:
            case rv64im::BLEZ:
            case rv64im::BGEZ:
            case rv64im::BLTZ:
            case rv64im::BGTZ:
           \s
                {
                    return true;
                }
                */
                default:
                {
                    return false;
                }
            }
            return false; // unreachable
        }
                
        bool rv64imMCInstExpander::isExpandable(const MCInst &MCI) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
            /*
            // instructions
           \s
                case rv64im::CALL:
                case rv64im::TAIL:
                case rv64im::RET:
                case rv64im::J:
                case rv64im::NOP:
                case rv64im::MOV:
                case rv64im::NOT:
                case rv64im::NEG:
                case rv64im::SNEZ:
                case rv64im::SLTZ:
                case rv64im::SGTZ:
                case rv64im::BEQZ:
                case rv64im::BNEZ:
                case rv64im::BLEZ:
                case rv64im::BGEZ:
                case rv64im::BLTZ:
                case rv64im::BGTZ:
           \s
                {
                    return true;
                }
                */
                default:
                {
                    return false;
                }
            }
            return false; // unreachable
        }
                
        bool rv64imMCInstExpander::expand(const MCInst &MCI, std::vector<MCInst> &MCIExpansion) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
                //
                // instructions
                //
                
            /*
           \s
              case rv64im::CALL:
              {
                MCIExpansion = RV64IM_CALL_expand(MCI);
                return true;
              }
              case rv64im::TAIL:
              {
                MCIExpansion = RV64IM_TAIL_expand(MCI);
                return true;
              }
              case rv64im::RET:
              {
                MCIExpansion = RV64IM_RET_expand(MCI);
                return true;
              }
              case rv64im::J:
              {
                MCIExpansion = RV64IM_J_expand(MCI);
                return true;
              }
              case rv64im::NOP:
              {
                MCIExpansion = RV64IM_NOP_expand(MCI);
                return true;
              }
              case rv64im::MOV:
              {
                MCIExpansion = RV64IM_MOV_expand(MCI);
                return true;
              }
              case rv64im::NOT:
              {
                MCIExpansion = RV64IM_NOT_expand(MCI);
                return true;
              }
              case rv64im::NEG:
              {
                MCIExpansion = RV64IM_NEG_expand(MCI);
                return true;
              }
              case rv64im::SNEZ:
              {
                MCIExpansion = RV64IM_SNEZ_expand(MCI);
                return true;
              }
              case rv64im::SLTZ:
              {
                MCIExpansion = RV64IM_SLTZ_expand(MCI);
                return true;
              }
              case rv64im::SGTZ:
              {
                MCIExpansion = RV64IM_SGTZ_expand(MCI);
                return true;
              }
              case rv64im::BEQZ:
              {
                MCIExpansion = RV64IM_BEQZ_expand(MCI);
                return true;
              }
              case rv64im::BNEZ:
              {
                MCIExpansion = RV64IM_BNEZ_expand(MCI);
                return true;
              }
              case rv64im::BLEZ:
              {
                MCIExpansion = RV64IM_BLEZ_expand(MCI);
                return true;
              }
              case rv64im::BGEZ:
              {
                MCIExpansion = RV64IM_BGEZ_expand(MCI);
                return true;
              }
              case rv64im::BLTZ:
              {
                MCIExpansion = RV64IM_BLTZ_expand(MCI);
                return true;
              }
              case rv64im::BGTZ:
              {
                MCIExpansion = RV64IM_BGTZ_expand(MCI);
                return true;
              }
           \s
            */
              default:
                {
                    return false;
                }
            }
            return false; // unreachable
        }
                
        const MCExpr *rv64imMCInstExpander::MCOperandToMCExpr(const MCOperand &MCO) const
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
                
        const int64_t rv64imMCInstExpander::MCOperandToInt64(const MCOperand &MCO) const
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
                
                
                
        std::vector< MCInst> RV64IM_CALL_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_TAIL_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_RET_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_J_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_NOP_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_MOV_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_NOT_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_NEG_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_SNEZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_SLTZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_SGTZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_BEQZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_BNEZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_BLEZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_BGEZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_BLTZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
                
                
        std::vector< MCInst> RV64IM_BGTZ_expand(const MCInst& instruction) {
        std::vector< MCInst > result;
        return result;
        }
        """.trim().lines(), output);
  }
}
