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

package vadl.lcb.template.Disassembler;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitDisassemblerCppFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitDisassemblerCppFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitDisassemblerCppFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines().map(String::trim);

    Assertions.assertLinesMatch("""
        #include "processornamevalueDisassembler.h"
        #include <iostream>
        #include "Utils/ImmediateUtils.h"
        
        #define DEBUG_TYPE "disassembler"
        
        using namespace llvm;
        
        processornamevalueDisassembler::processornamevalueDisassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian) : MCDisassembler(STI, Ctx), IsBigEndian(isBigEndian)
        {
        }
        
        /* == Register Classes == */
        
        static const unsigned XDecoderTable[] = {
        
            processornamevalue::X0,
            processornamevalue::X1,
            processornamevalue::X2,
            processornamevalue::X3,
            processornamevalue::X4,
            processornamevalue::X5,
            processornamevalue::X6,
            processornamevalue::X7,
            processornamevalue::X8,
            processornamevalue::X9,
            processornamevalue::X10,
            processornamevalue::X11,
            processornamevalue::X12,
            processornamevalue::X13,
            processornamevalue::X14,
            processornamevalue::X15,
            processornamevalue::X16,
            processornamevalue::X17,
            processornamevalue::X18,
            processornamevalue::X19,
            processornamevalue::X20,
            processornamevalue::X21,
            processornamevalue::X22,
            processornamevalue::X23,
            processornamevalue::X24,
            processornamevalue::X25,
            processornamevalue::X26,
            processornamevalue::X27,
            processornamevalue::X28,
            processornamevalue::X29,
            processornamevalue::X30,
            processornamevalue::X31
        
        };
        
        
        /* == Immediate Decoding == */
        
        DecodeStatus RV3264Base_ADDIW_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_ADDIW_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_ADDI_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_ADDI_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_ADDW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_ADDW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_ADD_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_ADD_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_ANDI_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_ANDI_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_AND_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_AND_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_AUIPC_immUp_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264Base_AUIPC_immUp_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_BEQ_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_BEQ_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_BGEU_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_BGEU_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_BGE_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_BGE_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_BLTU_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_BLTU_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_BLT_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_BLT_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_BNE_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_BNE_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_JALR_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_JALR_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_JAL_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264Base_JAL_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LBU_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LBU_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LB_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LB_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LD_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LD_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LHU_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LHU_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LH_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LH_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LUI_immUp_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264Base_LUI_immUp_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LWU_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LWU_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_LW_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_LW_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_ORI_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_ORI_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_OR_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_OR_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SB_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_SB_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SD_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_SD_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SH_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_SH_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLLIW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SLLIW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLLI_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 63;
            Imm = RV3264Base_SLLI_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLLW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SLLW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLL_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SLL_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLTIU_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_SLTIU_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLTI_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_SLTI_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLTU_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SLTU_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SLT_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SLT_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRAIW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SRAIW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRAI_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 63;
            Imm = RV3264Base_SRAI_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRAW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SRAW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRA_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SRA_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRLIW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SRLIW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRLI_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 63;
            Imm = RV3264Base_SRLI_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRLW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SRLW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SRL_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SRL_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SUBW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SUBW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SUB_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_SUB_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_SW_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_SW_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_XORI_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264Base_XORI_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264Base_XOR_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264Base_XOR_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_EBREAK_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_EBREAK_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_ECALL_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_ECALL_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_DIVUW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_DIVUW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_DIVU_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_DIVU_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_DIVW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_DIVW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_DIV_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_DIV_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_MULHSU_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_MULHSU_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_MULHU_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_MULHU_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_MULH_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_MULH_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_MULW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_MULW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_MUL_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_MUL_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_REMUW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_REMUW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_REMU_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_REMU_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_REMW_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_REMW_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264M_REM_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264M_REM_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        
        
        
        static DecodeStatus DecodeXRegisterClass
            ( MCInst &Inst
            , uint64_t RegNo
            , uint64_t Address
            , const void *Decoder
            )
        {
            // check if register number is in range
            if( RegNo >= 32)
                return MCDisassembler::Fail;
        
            // access custom generated decoder table in register info
            Register reg = XDecoderTable[RegNo];
        
            // check if decoded register is valid
            if( reg == processornamevalue::NoRegister )
                return MCDisassembler::Fail;
        
            Inst.addOperand( MCOperand::createReg(reg) );
            return MCDisassembler::Success;
        }
        
        
        #include "processornamevalueGenDisassemblerTables.inc"
        
        DecodeStatus processornamevalueDisassembler::getInstruction(MCInst &MI, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CS) const
        {
            if (Bytes.size() < 4)
            {
                Size = 0;
                return MCDisassembler::Fail;
            }
        
            uint32_t Instr;
        
        
                if (IsBigEndian)
                {
                    Instr = support::endian::read32be(Bytes.data());
                }
                else
                {
                    Instr = support::endian::read32le(Bytes.data());
                }
        
        
            auto Result = decodeInstruction(DecoderTable32, MI, Instr, Address, this, STI);
            Size = 4;
            return Result;
        }
        
        static MCDisassembler *createprocessornamevalueDisassembler(const Target &T, const MCSubtargetInfo &STI, MCContext &Ctx)
        {
            return new processornamevalueDisassembler(STI, Ctx, processornamevalueBaseInfo::IsBigEndian());
        }
        
        extern "C" void LLVMInitializeprocessornamevalueDisassembler()
        {
            // Register Target Disassembler
            TargetRegistry::RegisterMCDisassembler(getTheprocessornamevalueTarget(), createprocessornamevalueDisassembler);
        }
        """.trim().lines().map(String::trim), output);
  }
}
