// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.template.MCTargetDesc;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

@SuppressWarnings("checkstyle:FileTabCharacter")
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
        #include "processornamevalueMCInstExpander.h"
                
        #include "MCTargetDesc/processornamevalueMCTargetDesc.h"
        #include "Utils/ImmediateUtils.h"
                
        #include "MCTargetDesc/processornamevalueMCExpr.h"
        #include "llvm/MC/MCInst.h"
        #include "llvm/MC/MCExpr.h"
        #include "llvm/MC/MCContext.h"
                
        #define DEBUG_TYPE "processornamevalueMCInstExpander"
                
        using namespace llvm;
                
        processornamevalueMCInstExpander::processornamevalueMCInstExpander(class MCContext &Ctx)
            : Ctx(Ctx) {}
                
        bool processornamevalueMCInstExpander::needsExpansion(const MCInst &MCI) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
            // instructions
           \s
            case processornamevalue::CALL:
            case processornamevalue::TAIL:
            case processornamevalue::RET:
            case processornamevalue::J:
            case processornamevalue::NOP:
            case processornamevalue::MOV:
            case processornamevalue::NOT:
            case processornamevalue::NEG:
            case processornamevalue::SNEZ:
            case processornamevalue::SLTZ:
            case processornamevalue::SGTZ:
            case processornamevalue::BEQZ:
            case processornamevalue::BNEZ:
            case processornamevalue::BLEZ:
            case processornamevalue::BGEZ:
            case processornamevalue::BLTZ:
            case processornamevalue::BGTZ:
            case processornamevalue::LLA:
            case processornamevalue::LI:
            case processornamevalue::RESERVED_PSEUDO_RET:
            case processornamevalue::RESERVED_PSEUDO_CALL:
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
                
        bool processornamevalueMCInstExpander::isExpandable(const MCInst &MCI) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
            // instructions
           \s
                case processornamevalue::CALL:
                case processornamevalue::TAIL:
                case processornamevalue::RET:
                case processornamevalue::J:
                case processornamevalue::NOP:
                case processornamevalue::MOV:
                case processornamevalue::NOT:
                case processornamevalue::NEG:
                case processornamevalue::SNEZ:
                case processornamevalue::SLTZ:
                case processornamevalue::SGTZ:
                case processornamevalue::BEQZ:
                case processornamevalue::BNEZ:
                case processornamevalue::BLEZ:
                case processornamevalue::BGEZ:
                case processornamevalue::BLTZ:
                case processornamevalue::BGTZ:
                case processornamevalue::LLA:
                case processornamevalue::LI:
                case processornamevalue::RESERVED_PSEUDO_RET:
                case processornamevalue::RESERVED_PSEUDO_CALL:
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
                
        bool processornamevalueMCInstExpander::expand(const MCInst &MCI, std::vector<MCInst> &MCIExpansion) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
                //
                // instructions
                //
                
           \s
              case processornamevalue::CALL:
              {
                MCIExpansion = RV3264I_CALL_expand(MCI);
                return true;
              }
              case processornamevalue::TAIL:
              {
                MCIExpansion = RV3264I_TAIL_expand(MCI);
                return true;
              }
              case processornamevalue::RET:
              {
                MCIExpansion = RV3264I_RET_expand(MCI);
                return true;
              }
              case processornamevalue::J:
              {
                MCIExpansion = RV3264I_J_expand(MCI);
                return true;
              }
              case processornamevalue::NOP:
              {
                MCIExpansion = RV3264I_NOP_expand(MCI);
                return true;
              }
              case processornamevalue::MOV:
              {
                MCIExpansion = RV3264I_MOV_expand(MCI);
                return true;
              }
              case processornamevalue::NOT:
              {
                MCIExpansion = RV3264I_NOT_expand(MCI);
                return true;
              }
              case processornamevalue::NEG:
              {
                MCIExpansion = RV3264I_NEG_expand(MCI);
                return true;
              }
              case processornamevalue::SNEZ:
              {
                MCIExpansion = RV3264I_SNEZ_expand(MCI);
                return true;
              }
              case processornamevalue::SLTZ:
              {
                MCIExpansion = RV3264I_SLTZ_expand(MCI);
                return true;
              }
              case processornamevalue::SGTZ:
              {
                MCIExpansion = RV3264I_SGTZ_expand(MCI);
                return true;
              }
              case processornamevalue::BEQZ:
              {
                MCIExpansion = RV3264I_BEQZ_expand(MCI);
                return true;
              }
              case processornamevalue::BNEZ:
              {
                MCIExpansion = RV3264I_BNEZ_expand(MCI);
                return true;
              }
              case processornamevalue::BLEZ:
              {
                MCIExpansion = RV3264I_BLEZ_expand(MCI);
                return true;
              }
              case processornamevalue::BGEZ:
              {
                MCIExpansion = RV3264I_BGEZ_expand(MCI);
                return true;
              }
              case processornamevalue::BLTZ:
              {
                MCIExpansion = RV3264I_BLTZ_expand(MCI);
                return true;
              }
              case processornamevalue::BGTZ:
              {
                MCIExpansion = RV3264I_BGTZ_expand(MCI);
                return true;
              }
              case processornamevalue::LLA:
              {
                MCIExpansion = RV3264I_LLA_expand(MCI);
                return true;
              }
              case processornamevalue::LI:
              {
                MCIExpansion = RV3264I_LI_expand(MCI);
                return true;
              }
              case processornamevalue::RESERVED_PSEUDO_RET:
              {
                MCIExpansion = _RESERVED_PSEUDO_RET_expand(MCI);
                return true;
              }
              case processornamevalue::RESERVED_PSEUDO_CALL:
              {
                MCIExpansion = _RESERVED_PSEUDO_CALL_expand(MCI);
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
                
        const MCExpr *processornamevalueMCInstExpander::MCOperandToMCExpr(const MCOperand &MCO) const
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
                
        const int64_t processornamevalueMCInstExpander::MCOperandToInt64(const MCOperand &MCO) const
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
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_CALL_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::LUI);
              a.addOperand(MCOperand::createReg(processorNameValue::X1));
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi_Itype_imm, Ctx));
              a.addOperand(c);
              result.push_back(a);
              MCInst d = MCInst();
              d.setOpcode(processorNameValue::JALR);
              d.addOperand(MCOperand::createReg(processorNameValue::X1));
              d.addOperand(MCOperand::createReg(processorNameValue::X1));
              const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo_Itype_imm, Ctx));
              d.addOperand(f);
              result.push_back(d);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_TAIL_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::AUIPC);
              a.addOperand(MCOperand::createReg(processorNameValue::X6));
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi_Itype_imm, Ctx));
              a.addOperand(c);
              result.push_back(a);
              MCInst d = MCInst();
              d.setOpcode(processorNameValue::JALR);
              d.addOperand(MCOperand::createReg(processorNameValue::X0));
              d.addOperand(MCOperand::createReg(processorNameValue::X6));
              const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo_Itype_imm, Ctx));
              d.addOperand(f);
              result.push_back(d);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_RET_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::JALR);
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(MCOperand::createReg(processorNameValue::X1));
              a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_J_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::JAL);
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Jtype_imm, Ctx));
              a.addOperand(c);
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_NOP_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::ADDI);
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_MOV_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::ADDI);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(instruction.getOperand(1));
              a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_NOT_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::XORI);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(instruction.getOperand(1));
              a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(4095)));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_NEG_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::SUB);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(instruction.getOperand(1));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_SNEZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::SLTU);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(instruction.getOperand(1));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_SLTZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::SLT);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(instruction.getOperand(1));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_SGTZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::SLT);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(instruction.getOperand(1));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BEQZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::BEQ);
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Btype_imm, Ctx));
              a.addOperand(c);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BNEZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::BNE);
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Btype_imm, Ctx));
              a.addOperand(c);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BLEZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::BGE);
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Btype_imm, Ctx));
              a.addOperand(c);
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(instruction.getOperand(0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BGEZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::BGE);
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Btype_imm, Ctx));
              a.addOperand(c);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BLTZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::BLT);
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Btype_imm, Ctx));
              a.addOperand(c);
              a.addOperand(instruction.getOperand(0));
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BGTZ_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::BLT);
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_SYMB_ABS_RV3264I_Btype_imm, Ctx));
              a.addOperand(c);
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(instruction.getOperand(0));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LLA_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::LUI);
              a.addOperand(instruction.getOperand(0));
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi_Itype_imm, Ctx));
              a.addOperand(c);
              result.push_back(a);
              MCInst d = MCInst();
              d.setOpcode(processorNameValue::ADDI);
              d.addOperand(instruction.getOperand(0));
              d.addOperand(instruction.getOperand(0));
              const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo_Itype_imm, Ctx));
              d.addOperand(f);
              result.push_back(d);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LI_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::LUI);
              a.addOperand(instruction.getOperand(0));
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi_Itype_imm, Ctx));
              a.addOperand(c);
              result.push_back(a);
              MCInst d = MCInst();
              d.setOpcode(processorNameValue::ADDI);
              d.addOperand(instruction.getOperand(0));
              d.addOperand(instruction.getOperand(0));
              const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
              MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo_Itype_imm, Ctx));
              d.addOperand(f);
              result.push_back(d);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::_RESERVED_PSEUDO_RET_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::JALR);
              a.addOperand(MCOperand::createReg(processorNameValue::X0));
              a.addOperand(MCOperand::createReg(processorNameValue::X1));
              a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
              result.push_back(a);
              return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::_RESERVED_PSEUDO_CALL_expand(const MCInst& instruction) const
        {
              std::vector< MCInst > result;
              MCInst a = MCInst();
              a.setOpcode(processorNameValue::LUI);
              a.addOperand(MCOperand::createReg(processorNameValue::X1));
              const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi_Itype_imm, Ctx));
              a.addOperand(c);
              result.push_back(a);
              MCInst d = MCInst();
              d.setOpcode(processorNameValue::JALR);
              d.addOperand(MCOperand::createReg(processorNameValue::X1));
              d.addOperand(MCOperand::createReg(processorNameValue::X1));
              const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(0));
              MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo_Itype_imm, Ctx));
              d.addOperand(f);
              result.push_back(d);
              return result;
        }
        """.trim().lines(), output);
  }
}
