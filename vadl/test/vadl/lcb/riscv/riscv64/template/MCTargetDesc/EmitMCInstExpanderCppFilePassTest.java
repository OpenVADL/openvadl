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

package vadl.lcb.riscv.riscv64.template.MCTargetDesc;

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
        #include "llvm/MC/MCSymbol.h"
                
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
            case processornamevalue::LGA_64:
            case processornamevalue::CALL:
            case processornamevalue::TAIL:
            case processornamevalue::RET:
            case processornamevalue::J:
            case processornamevalue::NOP:
            case processornamevalue::MV:
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
            case processornamevalue::LA:
            case processornamevalue::LGA_32:
            case processornamevalue::LLA:
            case processornamevalue::LI:
            case processornamevalue::constMat0:
            case processornamevalue::constMat1:
            case processornamevalue::constMat2:
            case processornamevalue::constMat3:
            case processornamevalue::constMat4:
            case processornamevalue::registerAdjustment0:
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
                case processornamevalue::LGA_64:
                case processornamevalue::CALL:
                case processornamevalue::TAIL:
                case processornamevalue::RET:
                case processornamevalue::J:
                case processornamevalue::NOP:
                case processornamevalue::MV:
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
                case processornamevalue::LA:
                case processornamevalue::LGA_32:
                case processornamevalue::LLA:
                case processornamevalue::LI:
                case processornamevalue::constMat0:
                case processornamevalue::constMat1:
                case processornamevalue::constMat2:
                case processornamevalue::constMat3:
                case processornamevalue::constMat4:
                case processornamevalue::registerAdjustment0:
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
                
        bool processornamevalueMCInstExpander::expand(const MCInst &MCI,
          std::function<void(const MCInst &)> callback,
          std::function<void(MCSymbol*)> callbackSymbol  ) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
                //
                // instructions
                //
                
           \s
              case processornamevalue::LGA_64:
              {
                RV3264I_LGA_64_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::CALL:
              {
                RV3264I_CALL_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::TAIL:
              {
                RV3264I_TAIL_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::RET:
              {
                RV3264I_RET_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::J:
              {
                RV3264I_J_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::NOP:
              {
                RV3264I_NOP_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::MV:
              {
                RV3264I_MV_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::NOT:
              {
                RV3264I_NOT_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::NEG:
              {
                RV3264I_NEG_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::SNEZ:
              {
                RV3264I_SNEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::SLTZ:
              {
                RV3264I_SLTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::SGTZ:
              {
                RV3264I_SGTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BEQZ:
              {
                RV3264I_BEQZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BNEZ:
              {
                RV3264I_BNEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BLEZ:
              {
                RV3264I_BLEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BGEZ:
              {
                RV3264I_BGEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BLTZ:
              {
                RV3264I_BLTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BGTZ:
              {
                RV3264I_BGTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LA:
              {
                RV3264I_LA_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LGA_32:
              {
                RV3264I_LGA_32_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LLA:
              {
                RV3264I_LLA_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LI:
              {
                RV3264I_LI_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::constMat0:
              {
                constMat0_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::constMat1:
              {
                constMat1_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::constMat2:
              {
                constMat2_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::constMat3:
              {
                constMat3_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::constMat4:
              {
                constMat4_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::registerAdjustment0:
              {
                registerAdjustment0_expand(MCI, callback, callbackSymbol );
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
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LGA_64_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCSymbol *a = Ctx.createTempSymbol();
           callbackSymbol(a);
           MCInst b = MCInst();
           b.setOpcode(processorNameValue::AUIPC);
           b.addOperand(instruction.getOperand(0));
           const MCExpr* c = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand d = MCOperand::createExpr(processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_GOT_RV3264I_got_hi, Ctx));
           b.addOperand(d);
           result.push_back(b);
           callback(b);
           MCInst e = MCInst();
           e.setOpcode(processorNameValue::LD);
           e.addOperand(instruction.getOperand(0));
           e.addOperand(instruction.getOperand(0));
           const MCExpr* f = MCOperandToMCExpr(MCOperand::createExpr(MCSymbolRefExpr::create(a, Ctx)));
           MCOperand g = MCOperand::createExpr(processorNameValueMCExpr::create(f, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264I_pcrel_lo, Ctx));
           e.addOperand(g);
           result.push_back(e);
           callback(e);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_CALL_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(MCOperand::createReg(processorNameValue::X1));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::JALR);
           d.addOperand(MCOperand::createReg(processorNameValue::X1));
           d.addOperand(MCOperand::createReg(processorNameValue::X1));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(0));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_TAIL_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::AUIPC);
           a.addOperand(MCOperand::createReg(processorNameValue::X6));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::JALR);
           d.addOperand(MCOperand::createReg(processorNameValue::X0));
           d.addOperand(MCOperand::createReg(processorNameValue::X6));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(0));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_RET_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::JALR);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(MCOperand::createReg(processorNameValue::X1));
           a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_J_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::JAL);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_NOP_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_MV_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(instruction.getOperand(1));
           a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(0)));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_NOT_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::XORI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(instruction.getOperand(1));
           a.addOperand(MCOperand::createImm(RV3264I_Itype_immS_decode(4095)));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_NEG_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::SUB);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(instruction.getOperand(1));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_SNEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::SLTU);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(instruction.getOperand(1));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_SLTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::SLT);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(instruction.getOperand(1));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_SGTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::SLT);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(instruction.getOperand(1));
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BEQZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::BEQ);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BNEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::BNE);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BLEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::BGE);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BGEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::BGE);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BLTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::BLT);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_BGTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::BLT);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_None, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LA_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LGA_32_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::AUIPC);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_GOT_RV3264I_got_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::LW);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264I_pcrel_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LLA_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::AUIPC);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264I_pcrel_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264I_pcrel_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264I_LI_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::constMat0_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::constMat1_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_hi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::constMat2_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_to32AndHi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_to32AndLo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           MCInst g = MCInst();
           g.setOpcode(processorNameValue::SLLI);
           g.addOperand(instruction.getOperand(0));
           g.addOperand(instruction.getOperand(0));
           g.addOperand(MCOperand::createImm(RV3264I_Ftype_shamt_decode(16)));
           result.push_back(g);
           callback(g);
           MCInst h = MCInst();
           h.setOpcode(processorNameValue::ORI);
           h.addOperand(instruction.getOperand(0));
           h.addOperand(instruction.getOperand(0));
           const MCExpr* i = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand j = MCOperand::createExpr(processorNameValueMCExpr::create(i, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lowerHalfHi, Ctx));
           h.addOperand(j);
           result.push_back(h);
           callback(h);
           MCInst k = MCInst();
           k.setOpcode(processorNameValue::SLLI);
           k.addOperand(instruction.getOperand(0));
           k.addOperand(instruction.getOperand(0));
           k.addOperand(MCOperand::createImm(RV3264I_Ftype_shamt_decode(16)));
           result.push_back(k);
           callback(k);
           MCInst l = MCInst();
           l.setOpcode(processorNameValue::ORI);
           l.addOperand(instruction.getOperand(0));
           l.addOperand(instruction.getOperand(0));
           const MCExpr* m = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand n = MCOperand::createExpr(processorNameValueMCExpr::create(m, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lowerHalfLo, Ctx));
           l.addOperand(n);
           result.push_back(l);
           callback(l);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::constMat3_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_to32AndHi, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           MCInst d = MCInst();
           d.setOpcode(processorNameValue::ADDI);
           d.addOperand(instruction.getOperand(0));
           d.addOperand(instruction.getOperand(0));
           const MCExpr* e = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand f = MCOperand::createExpr(processorNameValueMCExpr::create(e, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_to32AndLo, Ctx));
           d.addOperand(f);
           result.push_back(d);
           callback(d);
           MCInst g = MCInst();
           g.setOpcode(processorNameValue::SLLI);
           g.addOperand(instruction.getOperand(0));
           g.addOperand(instruction.getOperand(0));
           g.addOperand(MCOperand::createImm(RV3264I_Ftype_shamt_decode(16)));
           result.push_back(g);
           callback(g);
           MCInst h = MCInst();
           h.setOpcode(processorNameValue::ORI);
           h.addOperand(instruction.getOperand(0));
           h.addOperand(instruction.getOperand(0));
           const MCExpr* i = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand j = MCOperand::createExpr(processorNameValueMCExpr::create(i, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lowerHalfHi, Ctx));
           h.addOperand(j);
           result.push_back(h);
           callback(h);
           MCInst k = MCInst();
           k.setOpcode(processorNameValue::SLLI);
           k.addOperand(instruction.getOperand(0));
           k.addOperand(instruction.getOperand(0));
           k.addOperand(MCOperand::createImm(RV3264I_Ftype_shamt_decode(16)));
           result.push_back(k);
           callback(k);
           MCInst l = MCInst();
           l.setOpcode(processorNameValue::ORI);
           l.addOperand(instruction.getOperand(0));
           l.addOperand(instruction.getOperand(0));
           const MCExpr* m = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand n = MCOperand::createExpr(processorNameValueMCExpr::create(m, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264I_lowerHalfLo, Ctx));
           l.addOperand(n);
           result.push_back(l);
           callback(l);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::constMat4_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264I_Itype_immS, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
                
                
                
        std::vector<MCInst> processorNameValueMCInstExpander::registerAdjustment0_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(instruction.getOperand(1));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(2));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264I_Itype_immS, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
        """.trim().lines(), output);
  }
}
