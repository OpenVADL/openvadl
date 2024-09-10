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
                
           \s
              case rv64im::CALL:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::CALL::expand](MCI);
                return true;
              }
              case rv64im::TAIL:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::TAIL::expand](MCI);
                return true;
              }
              case rv64im::RET:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::RET::expand](MCI);
                return true;
              }
              case rv64im::J:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::J::expand](MCI);
                return true;
              }
              case rv64im::NOP:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::NOP::expand](MCI);
                return true;
              }
              case rv64im::MOV:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::MOV::expand](MCI);
                return true;
              }
              case rv64im::NOT:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::NOT::expand](MCI);
                return true;
              }
              case rv64im::NEG:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::NEG::expand](MCI);
                return true;
              }
              case rv64im::SNEZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::SNEZ::expand](MCI);
                return true;
              }
              case rv64im::SLTZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::SLTZ::expand](MCI);
                return true;
              }
              case rv64im::SGTZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::SGTZ::expand](MCI);
                return true;
              }
              case rv64im::BEQZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::BEQZ::expand](MCI);
                return true;
              }
              case rv64im::BNEZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::BNEZ::expand](MCI);
                return true;
              }
              case rv64im::BLEZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::BLEZ::expand](MCI);
                return true;
              }
              case rv64im::BGEZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::BGEZ::expand](MCI);
                return true;
              }
              case rv64im::BLTZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::BLTZ::expand](MCI);
                return true;
              }
              case rv64im::BGTZ:
              {
                MCIExpansion = CppFunctionName[identifier=RV3264I::BGTZ::expand](MCI);
                return true;
              }
           \s
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
                
                
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_CALL_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::LUI);
        a.addOperand(MCOperand::createReg(processorNameValue::1));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_RV3264I_hi20, Ctx);
        a.addOperand(c);
        MCInst d = MCInst();
        d.setOpcode(processorNameValue::JALR);
        d.addOperand(MCOperand::createReg(processorNameValue::1));
        d.addOperand(MCOperand::createReg(processorNameValue::1));
        const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* f = CPUMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_RV3264I_lo12, Ctx);
        d.addOperand(f);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_TAIL_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::AUIPC);
        a.addOperand(MCOperand::createReg(processorNameValue::6));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_RV3264I_hi20, Ctx);
        a.addOperand(c);
        MCInst d = MCInst();
        d.setOpcode(processorNameValue::JALR);
        d.addOperand(MCOperand::createReg(processorNameValue::6));
        d.addOperand(MCOperand::createReg(processorNameValue::0));
        const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* f = CPUMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_RV3264I_lo12, Ctx);
        d.addOperand(f);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_RET_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::JALR);
        a.addOperand(MCOperand::createReg(processorNameValue::1));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode_decode(0)));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_J_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::JAL);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Jtype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_NOP_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::ADDI);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode_decode(0)));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_MOV_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::ADDI);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(instruction.getOperand(1));
        a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode_decode(0)));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_NOT_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::XORI);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(instruction.getOperand(1));
        a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode_decode(4095)));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_NEG_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::SUB);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(2));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_SNEZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::SLTU);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(2));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_SLTZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::SLT);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(1));
        a.addOperand(instruction.getOperand(2));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_SGTZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::SLT);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(2));
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_BEQZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::BEQ);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(1));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Btype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_BNEZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::BNE);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(1));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Btype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_BLEZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::BGE);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Btype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_BGEZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::BGE);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(1));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Btype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_BLTZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::BLT);
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        a.addOperand(instruction.getOperand(1));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Btype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        CppFunctionCode[value=std::vector< MCInst&> RV3264I_BGTZ_expand(const MCInst instruction) {
        std::vector< MCInst > result;
        MCInst a = MCInst();
        a.setOpcode(processorNameValue::BLT);
        a.addOperand(instruction.getOperand(0));
        a.addOperand(MCOperand::createReg(processorNameValue::0));
        const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
        const MCExpr* c = CPUMCExpr::create(b, processorNameValueMCExpr::VariantKind::VariantKind[value=VK_RV3264I_Btype_imm], Ctx);
        a.addOperand(c);
        return result;;
        }]
        """.trim().lines(), output);
  }
}
