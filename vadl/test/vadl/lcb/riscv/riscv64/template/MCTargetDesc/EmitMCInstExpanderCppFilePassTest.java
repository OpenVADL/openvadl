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
            case processornamevalue::registerAdjustment0:
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
        
        bool processornamevalueMCInstExpander::isExpandableForAssembly(const MCInst &MCI) const
        {
            auto opcode = MCI.getOpcode();
            switch (opcode)
            {
            case processornamevalue::constMat0:
            case processornamevalue::constMat1:
            case processornamevalue::constMat2:
            case processornamevalue::registerAdjustment0:
            case processornamevalue::LA:
            case processornamevalue::LLA:
            case processornamevalue::LGA_64:
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
            case processornamevalue::registerAdjustment0:
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
              case processornamevalue::LGA_64:
              {
                RV3264Base_LGA_64_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::CALL:
              {
                RV3264Base_CALL_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::TAIL:
              {
                RV3264Base_TAIL_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::RET:
              {
                RV3264Base_RET_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::J:
              {
                RV3264Base_J_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::NOP:
              {
                RV3264Base_NOP_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::MV:
              {
                RV3264Base_MV_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::NOT:
              {
                RV3264Base_NOT_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::NEG:
              {
                RV3264Base_NEG_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::SNEZ:
              {
                RV3264Base_SNEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::SLTZ:
              {
                RV3264Base_SLTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::SGTZ:
              {
                RV3264Base_SGTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BEQZ:
              {
                RV3264Base_BEQZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BNEZ:
              {
                RV3264Base_BNEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BLEZ:
              {
                RV3264Base_BLEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BGEZ:
              {
                RV3264Base_BGEZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BLTZ:
              {
                RV3264Base_BLTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::BGTZ:
              {
                RV3264Base_BGTZ_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LA:
              {
                RV3264Base_LA_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LGA_32:
              {
                RV3264Base_LGA_32_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LLA:
              {
                RV3264Base_LLA_expand(MCI, callback, callbackSymbol );
                return true;
              }
              case processornamevalue::LI:
              {
                RV3264Base_LI_expand(MCI, callback, callbackSymbol );
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
              case processornamevalue::registerAdjustment0:
              {
                registerAdjustment0_expand(MCI, callback, callbackSymbol );
                return true;
              }
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_LGA_64_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCSymbol *a = Ctx.createTempSymbol();
           callbackSymbol(a);
           MCInst b = MCInst();
           b.setOpcode(processorNameValue::AUIPC);
           b.addOperand(instruction.getOperand(0));
           const MCExpr* c = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_GOT_RV3264Base_got_pcrel_hi, Ctx);
           const MCExpr* e = processorNameValueMCExpr::create(d, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand f = MCOperand::createExpr(e);
           b.addOperand(f);
           result.push_back(b);
           callback(b);
           MCInst g = MCInst();
           g.setOpcode(processorNameValue::LD);
           g.addOperand(instruction.getOperand(0));
           g.addOperand(instruction.getOperand(0));
           const MCExpr* h = MCOperandToMCExpr(MCOperand::createExpr(MCSymbolRefExpr::create(a, Ctx)));
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264Base_pcrel_lo, Ctx);
           const MCExpr* j = processorNameValueMCExpr::create(i, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand k = MCOperand::createExpr(j);
           g.addOperand(k);
           result.push_back(g);
           callback(g);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_CALL_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(MCOperand::createReg(processorNameValue::X1));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::JALR);
           f.addOperand(MCOperand::createReg(processorNameValue::X1));
           f.addOperand(MCOperand::createReg(processorNameValue::X1));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(0));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_TAIL_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::AUIPC);
           a.addOperand(MCOperand::createReg(processorNameValue::X6));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(0));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::JALR);
           f.addOperand(MCOperand::createReg(processorNameValue::X0));
           f.addOperand(MCOperand::createReg(processorNameValue::X6));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(0));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_RET_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::JALR);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(MCOperand::createReg(processorNameValue::X1));
           a.addOperand(MCOperand::createImm(RV3264Base_Itype_immS_decode(0)));
           result.push_back(a);
           callback(a);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_J_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_NOP_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           a.addOperand(MCOperand::createImm(RV3264Base_Itype_immS_decode(0)));
           result.push_back(a);
           callback(a);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_MV_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(instruction.getOperand(1));
           a.addOperand(MCOperand::createImm(RV3264Base_Itype_immS_decode(0)));
           result.push_back(a);
           callback(a);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_NOT_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::XORI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(instruction.getOperand(1));
           a.addOperand(MCOperand::createImm(RV3264Base_Itype_immS_decode(4095)));
           result.push_back(a);
           callback(a);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_NEG_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_SNEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_SLTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_SGTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_BEQZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_BNEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_BLEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_BGEZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_BLTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_BGTZ_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
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
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_LA_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::ADDI);
           f.addOperand(instruction.getOperand(0));
           f.addOperand(instruction.getOperand(0));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_LGA_32_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::AUIPC);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_GOT_RV3264Base_got_pcrel_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::LW);
           f.addOperand(instruction.getOperand(0));
           f.addOperand(instruction.getOperand(0));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264Base_pcrel_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_LLA_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCSymbol *a = Ctx.createTempSymbol();
           callbackSymbol(a);
           MCInst b = MCInst();
           b.setOpcode(processorNameValue::AUIPC);
           b.addOperand(instruction.getOperand(0));
           const MCExpr* c = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264Base_pcrel_hi, Ctx);
           const MCExpr* e = processorNameValueMCExpr::create(d, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand f = MCOperand::createExpr(e);
           b.addOperand(f);
           result.push_back(b);
           callback(b);
           MCInst g = MCInst();
           g.setOpcode(processorNameValue::ADDI);
           g.addOperand(instruction.getOperand(0));
           g.addOperand(instruction.getOperand(0));
           const MCExpr* h = MCOperandToMCExpr(MCOperand::createExpr(MCSymbolRefExpr::create(a, Ctx)));
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_PCREL_RV3264Base_pcrel_lo, Ctx);
           const MCExpr* j = processorNameValueMCExpr::create(i, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand k = MCOperand::createExpr(j);
           g.addOperand(k);
           result.push_back(g);
           callback(g);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::RV3264Base_LI_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::ADDI);
           f.addOperand(instruction.getOperand(0));
           f.addOperand(instruction.getOperand(0));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::constMat0_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::ADDI);
           f.addOperand(instruction.getOperand(0));
           f.addOperand(instruction.getOperand(0));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::constMat1_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::LUI);
           a.addOperand(instruction.getOperand(0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* c = processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_hi, Ctx);
           const MCExpr* d = processorNameValueMCExpr::create(c, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Utype_immUp, Ctx);
           MCOperand e = MCOperand::createExpr(d);
           a.addOperand(e);
           result.push_back(a);
           callback(a);
           MCInst f = MCInst();
           f.setOpcode(processorNameValue::ADDI);
           f.addOperand(instruction.getOperand(0));
           f.addOperand(instruction.getOperand(0));
           const MCExpr* g = MCOperandToMCExpr(instruction.getOperand(1));
           const MCExpr* h = processorNameValueMCExpr::create(g, processorNameValueMCExpr::VariantKind::VK_ABS_RV3264Base_lo, Ctx);
           const MCExpr* i = processorNameValueMCExpr::create(h, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx);
           MCOperand j = MCOperand::createExpr(i);
           f.addOperand(j);
           result.push_back(f);
           callback(f);
           return result;
        }
        
        
        
        std::vector<MCInst> processorNameValueMCInstExpander::constMat2_expand(const MCInst& instruction, std::function<void(const MCInst &)> callback, std::function<void(MCSymbol* )> callbackSymbol ) const
        {
           std::vector< MCInst > result;
           MCInst a = MCInst();
           a.setOpcode(processorNameValue::ADDI);
           a.addOperand(instruction.getOperand(0));
           a.addOperand(MCOperand::createReg(processorNameValue::X0));
           const MCExpr* b = MCOperandToMCExpr(instruction.getOperand(1));
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx));
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
           MCOperand c = MCOperand::createExpr(processorNameValueMCExpr::create(b, processorNameValueMCExpr::VariantKind::VK_DECODE_RV3264Base_Itype_immS, Ctx));
           a.addOperand(c);
           result.push_back(a);
           callback(a);
           return result;
        }
        """.trim().lines(), output);
  }
}
