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

package vadl.lcb.riscv.riscv64.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitImmediateFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var testSetup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(EmitImmediateFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitImmediateFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines().map(String::trim);

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_processornamevalue_UTILS_IMMEDIATEUTILS_H
        #define LLVM_LIB_TARGET_processornamevalue_UTILS_IMMEDIATEUTILS_H
        
        #include "llvm/Support/ErrorHandling.h"
        #include "llvm/MC/MCInst.h"
        #include "vadl-builtins.h"
        #include <cstdint>
        #include <unordered_map>
        #include <vector>
        #include <stdio.h>
        #include <bitset>
        
        // "__extension__" suppresses warning
        __extension__ typedef          __int128 int128_t;
        __extension__ typedef unsigned __int128 uint128_t;
        
        template<int start, int end, std::size_t N>
        std::bitset<N> project_range(std::bitset<N> bits)
        {
        std::bitset<N> result;
        size_t result_index = 0; // Index for the new bitset
        
        // Extract bits from the range [start, end]
        for (size_t i = start; i <= end; ++i) {
        result[result_index] = bits[i];
        result_index++;
        }
        
        return result;
        }
        
        
        static int64_t RV3264Base_ADDIW_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_ADDI_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_ANDI_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_AUIPC_immUp_decode(uint32_t imm) {
        return VADL_lsl(VADL_sextract(imm, 20), 64, ((uint8_t) 0xc ), 4);
        }
        static int64_t RV3264Base_BEQ_immS_decode(uint16_t imm) {
        return VADL_lsl(VADL_sextract(imm, 12), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_BGEU_immS_decode(uint16_t imm) {
        return VADL_lsl(VADL_sextract(imm, 12), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_BGE_immS_decode(uint16_t imm) {
        return VADL_lsl(VADL_sextract(imm, 12), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_BLTU_immS_decode(uint16_t imm) {
        return VADL_lsl(VADL_sextract(imm, 12), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_BLT_immS_decode(uint16_t imm) {
        return VADL_lsl(VADL_sextract(imm, 12), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_BNE_immS_decode(uint16_t imm) {
        return VADL_lsl(VADL_sextract(imm, 12), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_JALR_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_JAL_immS_decode(uint32_t imm) {
        return VADL_lsl(VADL_sextract(imm, 20), 64, ((uint8_t) 0x1 ), 1);
        }
        static int64_t RV3264Base_LBU_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_LB_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_LD_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_LHU_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_LH_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_LUI_immUp_decode(uint32_t imm) {
        return VADL_lsl(VADL_sextract(imm, 20), 64, ((uint8_t) 0xc ), 4);
        }
        static int64_t RV3264Base_LWU_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_LW_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_ORI_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_SB_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_SD_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_SH_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_SLTIU_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_SLTI_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_SW_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264Base_XORI_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264I_EBREAK_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static int64_t RV3264I_ECALL_immS_decode(uint16_t imm) {
        return VADL_sextract(imm, 12);
        }
        static uint8_t RV3264Base_ADDW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_ADD_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_AND_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_OR_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SLLIW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SLLI_shamt_decode(uint8_t sft) {
        return sft;
        }
        static uint8_t RV3264Base_SLLW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SLL_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SLTU_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SLT_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SRAIW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SRAI_shamt_decode(uint8_t sft) {
        return sft;
        }
        static uint8_t RV3264Base_SRAW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SRA_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SRLIW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SRLI_shamt_decode(uint8_t sft) {
        return sft;
        }
        static uint8_t RV3264Base_SRLW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SRL_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SUBW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_SUB_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264Base_XOR_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_DIVUW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_DIVU_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_DIVW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_DIV_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_MULHSU_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_MULHU_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_MULH_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_MULW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_MUL_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_REMUW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_REMU_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_REMW_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        static uint8_t RV3264M_REM_shamt_decode(uint8_t rs2) {
        return rs2;
        }
        
        
        
        static uint16_t RV3264Base_ADDIW_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_ADDI_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_ANDI_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_BEQ_imm(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_BGEU_imm(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_BGE_imm(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_BLTU_imm(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_BLT_imm(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_BNE_imm(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_JALR_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LBU_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LB_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LD_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LHU_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LH_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LWU_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_LW_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_ORI_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_SB_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_SD_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_SH_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_SLTIU_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_SLTI_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_SW_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint16_t RV3264Base_XORI_imm(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint32_t RV3264Base_AUIPC_imm(int64_t immUp) {
        return (project_range<12, 31>(std::bitset<64>(immUp)) << 0).to_ulong();
        }
        static uint32_t RV3264Base_JAL_imm(int64_t immS) {
        return (project_range<1, 20>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        static uint32_t RV3264Base_LUI_imm(int64_t immUp) {
        return (project_range<12, 31>(std::bitset<64>(immUp)) << 0).to_ulong();
        }
        static uint8_t RV3264Base_SLLIW_rs2(uint8_t shamt) {
        return (project_range<0, 4>(std::bitset<5>(shamt)) << 0).to_ulong();
        }
        static uint8_t RV3264Base_SLLI_sft(uint8_t shamt) {
        return (project_range<0, 5>(std::bitset<6>(shamt)) << 0).to_ulong();
        }
        static uint8_t RV3264Base_SRAIW_rs2(uint8_t shamt) {
        return (project_range<0, 4>(std::bitset<5>(shamt)) << 0).to_ulong();
        }
        static uint8_t RV3264Base_SRAI_sft(uint8_t shamt) {
        return (project_range<0, 5>(std::bitset<6>(shamt)) << 0).to_ulong();
        }
        static uint8_t RV3264Base_SRLIW_rs2(uint8_t shamt) {
        return (project_range<0, 4>(std::bitset<5>(shamt)) << 0).to_ulong();
        }
        static uint8_t RV3264Base_SRLI_sft(uint8_t shamt) {
        return (project_range<0, 5>(std::bitset<6>(shamt)) << 0).to_ulong();
        }
        
        
        
        
        static bool RV3264Base_ADDIW_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_ADDI_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_ADDW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_ADD_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_ANDI_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_AND_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_AUIPC_immUp_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_BEQ_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_BGEU_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_BGE_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_BLTU_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_BLT_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_BNE_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_JALR_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_JAL_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LBU_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LB_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LD_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LHU_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LH_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LUI_immUp_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LWU_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_LW_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_ORI_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_OR_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SB_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SD_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SH_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLLIW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLLI_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLLW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLL_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLTIU_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLTI_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLTU_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SLT_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRAIW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRAI_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRAW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRA_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRLIW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRLI_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRLW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SRL_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SUBW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SUB_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_SW_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_XORI_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264Base_XOR_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264I_EBREAK_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264I_ECALL_immS_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_DIVUW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_DIVU_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_DIVW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_DIV_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_MULHSU_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_MULHU_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_MULH_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_MULW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_MUL_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_REMUW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_REMU_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_REMW_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        static bool RV3264M_REM_shamt_predicate(uint64_t param) {
        return ((bool) 0x1 );
        }
        
        
        namespace
        {
        class ImmediateUtils
        {
        public:
        // Enum to control which immediate functions to use.
        // Currently this is only used in the pseudo expansion pass.
        enum processornamevalueImmediateKind{IK_UNKNOWN_IMMEDIATE // used for side effect registers which are interpreted as immediate
        
        , IK_RV3264Base_ADDIW_immS_decode
        , IK_RV3264Base_ADDI_immS_decode
        , IK_RV3264Base_ADDW_shamt_decode
        , IK_RV3264Base_ADD_shamt_decode
        , IK_RV3264Base_ANDI_immS_decode
        , IK_RV3264Base_AND_shamt_decode
        , IK_RV3264Base_AUIPC_immUp_decode
        , IK_RV3264Base_BEQ_immS_decode
        , IK_RV3264Base_BGEU_immS_decode
        , IK_RV3264Base_BGE_immS_decode
        , IK_RV3264Base_BLTU_immS_decode
        , IK_RV3264Base_BLT_immS_decode
        , IK_RV3264Base_BNE_immS_decode
        , IK_RV3264Base_JALR_immS_decode
        , IK_RV3264Base_JAL_immS_decode
        , IK_RV3264Base_LBU_immS_decode
        , IK_RV3264Base_LB_immS_decode
        , IK_RV3264Base_LD_immS_decode
        , IK_RV3264Base_LHU_immS_decode
        , IK_RV3264Base_LH_immS_decode
        , IK_RV3264Base_LUI_immUp_decode
        , IK_RV3264Base_LWU_immS_decode
        , IK_RV3264Base_LW_immS_decode
        , IK_RV3264Base_ORI_immS_decode
        , IK_RV3264Base_OR_shamt_decode
        , IK_RV3264Base_SB_immS_decode
        , IK_RV3264Base_SD_immS_decode
        , IK_RV3264Base_SH_immS_decode
        , IK_RV3264Base_SLLIW_shamt_decode
        , IK_RV3264Base_SLLI_shamt_decode
        , IK_RV3264Base_SLLW_shamt_decode
        , IK_RV3264Base_SLL_shamt_decode
        , IK_RV3264Base_SLTIU_immS_decode
        , IK_RV3264Base_SLTI_immS_decode
        , IK_RV3264Base_SLTU_shamt_decode
        , IK_RV3264Base_SLT_shamt_decode
        , IK_RV3264Base_SRAIW_shamt_decode
        , IK_RV3264Base_SRAI_shamt_decode
        , IK_RV3264Base_SRAW_shamt_decode
        , IK_RV3264Base_SRA_shamt_decode
        , IK_RV3264Base_SRLIW_shamt_decode
        , IK_RV3264Base_SRLI_shamt_decode
        , IK_RV3264Base_SRLW_shamt_decode
        , IK_RV3264Base_SRL_shamt_decode
        , IK_RV3264Base_SUBW_shamt_decode
        , IK_RV3264Base_SUB_shamt_decode
        , IK_RV3264Base_SW_immS_decode
        , IK_RV3264Base_XORI_immS_decode
        , IK_RV3264Base_XOR_shamt_decode
        , IK_RV3264I_EBREAK_immS_decode
        , IK_RV3264I_ECALL_immS_decode
        , IK_RV3264M_DIVUW_shamt_decode
        , IK_RV3264M_DIVU_shamt_decode
        , IK_RV3264M_DIVW_shamt_decode
        , IK_RV3264M_DIV_shamt_decode
        , IK_RV3264M_MULHSU_shamt_decode
        , IK_RV3264M_MULHU_shamt_decode
        , IK_RV3264M_MULH_shamt_decode
        , IK_RV3264M_MULW_shamt_decode
        , IK_RV3264M_MUL_shamt_decode
        , IK_RV3264M_REMUW_shamt_decode
        , IK_RV3264M_REMU_shamt_decode
        , IK_RV3264M_REMW_shamt_decode
        , IK_RV3264M_REM_shamt_decode
        
        };
        
        static uint64_t applyDecoding(const uint64_t value, processornamevalueImmediateKind kind)
        {
        switch (kind)
        {
        default:
        llvm_unreachable("Unsupported immediate kind to use for decoding!");
        case IK_UNKNOWN_IMMEDIATE:
        return value;
        
        case IK_RV3264Base_ADDIW_immS_decode:
        return RV3264Base_ADDIW_immS_decode(value);
        case IK_RV3264Base_ADDI_immS_decode:
        return RV3264Base_ADDI_immS_decode(value);
        case IK_RV3264Base_ADDW_shamt_decode:
        return RV3264Base_ADDW_shamt_decode(value);
        case IK_RV3264Base_ADD_shamt_decode:
        return RV3264Base_ADD_shamt_decode(value);
        case IK_RV3264Base_ANDI_immS_decode:
        return RV3264Base_ANDI_immS_decode(value);
        case IK_RV3264Base_AND_shamt_decode:
        return RV3264Base_AND_shamt_decode(value);
        case IK_RV3264Base_AUIPC_immUp_decode:
        return RV3264Base_AUIPC_immUp_decode(value);
        case IK_RV3264Base_BEQ_immS_decode:
        return RV3264Base_BEQ_immS_decode(value);
        case IK_RV3264Base_BGEU_immS_decode:
        return RV3264Base_BGEU_immS_decode(value);
        case IK_RV3264Base_BGE_immS_decode:
        return RV3264Base_BGE_immS_decode(value);
        case IK_RV3264Base_BLTU_immS_decode:
        return RV3264Base_BLTU_immS_decode(value);
        case IK_RV3264Base_BLT_immS_decode:
        return RV3264Base_BLT_immS_decode(value);
        case IK_RV3264Base_BNE_immS_decode:
        return RV3264Base_BNE_immS_decode(value);
        case IK_RV3264Base_JALR_immS_decode:
        return RV3264Base_JALR_immS_decode(value);
        case IK_RV3264Base_JAL_immS_decode:
        return RV3264Base_JAL_immS_decode(value);
        case IK_RV3264Base_LBU_immS_decode:
        return RV3264Base_LBU_immS_decode(value);
        case IK_RV3264Base_LB_immS_decode:
        return RV3264Base_LB_immS_decode(value);
        case IK_RV3264Base_LD_immS_decode:
        return RV3264Base_LD_immS_decode(value);
        case IK_RV3264Base_LHU_immS_decode:
        return RV3264Base_LHU_immS_decode(value);
        case IK_RV3264Base_LH_immS_decode:
        return RV3264Base_LH_immS_decode(value);
        case IK_RV3264Base_LUI_immUp_decode:
        return RV3264Base_LUI_immUp_decode(value);
        case IK_RV3264Base_LWU_immS_decode:
        return RV3264Base_LWU_immS_decode(value);
        case IK_RV3264Base_LW_immS_decode:
        return RV3264Base_LW_immS_decode(value);
        case IK_RV3264Base_ORI_immS_decode:
        return RV3264Base_ORI_immS_decode(value);
        case IK_RV3264Base_OR_shamt_decode:
        return RV3264Base_OR_shamt_decode(value);
        case IK_RV3264Base_SB_immS_decode:
        return RV3264Base_SB_immS_decode(value);
        case IK_RV3264Base_SD_immS_decode:
        return RV3264Base_SD_immS_decode(value);
        case IK_RV3264Base_SH_immS_decode:
        return RV3264Base_SH_immS_decode(value);
        case IK_RV3264Base_SLLIW_shamt_decode:
        return RV3264Base_SLLIW_shamt_decode(value);
        case IK_RV3264Base_SLLI_shamt_decode:
        return RV3264Base_SLLI_shamt_decode(value);
        case IK_RV3264Base_SLLW_shamt_decode:
        return RV3264Base_SLLW_shamt_decode(value);
        case IK_RV3264Base_SLL_shamt_decode:
        return RV3264Base_SLL_shamt_decode(value);
        case IK_RV3264Base_SLTIU_immS_decode:
        return RV3264Base_SLTIU_immS_decode(value);
        case IK_RV3264Base_SLTI_immS_decode:
        return RV3264Base_SLTI_immS_decode(value);
        case IK_RV3264Base_SLTU_shamt_decode:
        return RV3264Base_SLTU_shamt_decode(value);
        case IK_RV3264Base_SLT_shamt_decode:
        return RV3264Base_SLT_shamt_decode(value);
        case IK_RV3264Base_SRAIW_shamt_decode:
        return RV3264Base_SRAIW_shamt_decode(value);
        case IK_RV3264Base_SRAI_shamt_decode:
        return RV3264Base_SRAI_shamt_decode(value);
        case IK_RV3264Base_SRAW_shamt_decode:
        return RV3264Base_SRAW_shamt_decode(value);
        case IK_RV3264Base_SRA_shamt_decode:
        return RV3264Base_SRA_shamt_decode(value);
        case IK_RV3264Base_SRLIW_shamt_decode:
        return RV3264Base_SRLIW_shamt_decode(value);
        case IK_RV3264Base_SRLI_shamt_decode:
        return RV3264Base_SRLI_shamt_decode(value);
        case IK_RV3264Base_SRLW_shamt_decode:
        return RV3264Base_SRLW_shamt_decode(value);
        case IK_RV3264Base_SRL_shamt_decode:
        return RV3264Base_SRL_shamt_decode(value);
        case IK_RV3264Base_SUBW_shamt_decode:
        return RV3264Base_SUBW_shamt_decode(value);
        case IK_RV3264Base_SUB_shamt_decode:
        return RV3264Base_SUB_shamt_decode(value);
        case IK_RV3264Base_SW_immS_decode:
        return RV3264Base_SW_immS_decode(value);
        case IK_RV3264Base_XORI_immS_decode:
        return RV3264Base_XORI_immS_decode(value);
        case IK_RV3264Base_XOR_shamt_decode:
        return RV3264Base_XOR_shamt_decode(value);
        case IK_RV3264I_EBREAK_immS_decode:
        return RV3264I_EBREAK_immS_decode(value);
        case IK_RV3264I_ECALL_immS_decode:
        return RV3264I_ECALL_immS_decode(value);
        case IK_RV3264M_DIVUW_shamt_decode:
        return RV3264M_DIVUW_shamt_decode(value);
        case IK_RV3264M_DIVU_shamt_decode:
        return RV3264M_DIVU_shamt_decode(value);
        case IK_RV3264M_DIVW_shamt_decode:
        return RV3264M_DIVW_shamt_decode(value);
        case IK_RV3264M_DIV_shamt_decode:
        return RV3264M_DIV_shamt_decode(value);
        case IK_RV3264M_MULHSU_shamt_decode:
        return RV3264M_MULHSU_shamt_decode(value);
        case IK_RV3264M_MULHU_shamt_decode:
        return RV3264M_MULHU_shamt_decode(value);
        case IK_RV3264M_MULH_shamt_decode:
        return RV3264M_MULH_shamt_decode(value);
        case IK_RV3264M_MULW_shamt_decode:
        return RV3264M_MULW_shamt_decode(value);
        case IK_RV3264M_MUL_shamt_decode:
        return RV3264M_MUL_shamt_decode(value);
        case IK_RV3264M_REMUW_shamt_decode:
        return RV3264M_REMUW_shamt_decode(value);
        case IK_RV3264M_REMU_shamt_decode:
        return RV3264M_REMU_shamt_decode(value);
        case IK_RV3264M_REMW_shamt_decode:
        return RV3264M_REMW_shamt_decode(value);
        case IK_RV3264M_REM_shamt_decode:
        return RV3264M_REM_shamt_decode(value);
        
        }
        }
        };
        
        } // end of anonymous namespace
        
        #endif // LLVM_LIB_TARGET_processornamevalue_UTILS_IMMEDIATEUTILS_H
        """.trim().lines().map(String::trim), output);
  }
}
