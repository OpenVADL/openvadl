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

public class EmitLldTargetRelocationsHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(
            EmitLldTargetRelocationsHeaderFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(
                EmitLldTargetRelocationsHeaderFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        uint32_t RV3264I_Btype_ABSOLUTE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Btype_RELATIVE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Ftype_ABSOLUTE_sft(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Ftype_RELATIVE_sft(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Itype_ABSOLUTE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Itype_RELATIVE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Jtype_ABSOLUTE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Jtype_RELATIVE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Stype_ABSOLUTE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Stype_RELATIVE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Utype_ABSOLUTE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_Utype_RELATIVE_imm(uint32_t input) {
           return input;
        }
        uint32_t RV3264I_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        uint32_t RV3264I_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        uint32_t RV3264I_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        uint32_t RV3264I_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        uint32_t RV3264I_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        uint32_t RV3264I_hi(uint32_t symbol) {
           return VADL_uextract(VADL_lsr(VADL_add(symbol, 32, ((uint32_t) 0x00000800 ), 32), 32, ((uint8_t) 0xc ), 4), 20);
        }
        int16_t RV3264I_lo(uint32_t symbol) {
           return VADL_uextract(symbol, 12);
        }
        int16_t RV3264I_lo(uint32_t symbol) {
           return VADL_uextract(symbol, 12);
        }
        int16_t RV3264I_lo(uint32_t symbol) {
           return VADL_uextract(symbol, 12);
        }
        int16_t RV3264I_lo(uint32_t symbol) {
           return VADL_uextract(symbol, 12);
        }
        int16_t RV3264I_lo(uint32_t symbol) {
           return VADL_uextract(symbol, 12);
        }
        int16_t RV3264I_lo(uint32_t symbol) {
           return VADL_uextract(symbol, 12);
        }
        """.trim().lines(), output);
  }
}
