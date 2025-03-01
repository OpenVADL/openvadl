package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitLldManualEncodingHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(
            EmitLldManualEncodingHeaderFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(
                EmitLldManualEncodingHeaderFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #include <cstdint>
        #include <iostream>
        #include <bitset>
        #include <vector>
        #include <tuple>
                
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
                
        template<std::size_t N, std::size_t M>
        std::bitset<N> set_bits(std::bitset<N> dest, const std::bitset<M> source, std::vector<int> bits) {
            auto target = 0;
            for (int i = bits.size() - 1; i >= 0 ; --i) {
                auto j = bits[target];
                dest.set(j, source[i]);
                target++;
            }
                
            return dest;
        }
                
                
                
        uint32_t RV3264I_Utype_imm(uint32_t instWord, uint32_t newValue) {
           return set_bits(std::bitset<32>(instWord), std::bitset<32>(newValue), std::vector<int> { 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12 } ).to_ulong();
        }
        uint32_t RV3264I_Btype_imm(uint32_t instWord, uint32_t newValue) {
           return set_bits(std::bitset<32>(instWord), std::bitset<32>(newValue), std::vector<int> { 31, 7, 30, 29, 28, 27, 26, 25, 11, 10, 9, 8 } ).to_ulong();
        }
        uint32_t RV3264I_Ftype_sft(uint32_t instWord, uint32_t newValue) {
           return set_bits(std::bitset<32>(instWord), std::bitset<32>(newValue), std::vector<int> { 25, 24, 23, 22, 21, 20 } ).to_ulong();
        }
        uint32_t RV3264I_Stype_imm(uint32_t instWord, uint32_t newValue) {
           return set_bits(std::bitset<32>(instWord), std::bitset<32>(newValue), std::vector<int> { 31, 30, 29, 28, 27, 26, 25, 11, 10, 9, 8, 7 } ).to_ulong();
        }
        uint32_t RV3264I_Jtype_imm(uint32_t instWord, uint32_t newValue) {
           return set_bits(std::bitset<32>(instWord), std::bitset<32>(newValue), std::vector<int> { 31, 19, 18, 17, 16, 15, 14, 13, 12, 20, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21 } ).to_ulong();
        }
        uint32_t RV3264I_Itype_imm(uint32_t instWord, uint32_t newValue) {
           return set_bits(std::bitset<32>(instWord), std::bitset<32>(newValue), std::vector<int> { 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20 } ).to_ulong();
        }
        """.trim().lines(), output);
  }
}
