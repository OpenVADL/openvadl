package vadl.test.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

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
        uint32_t RV64IM_Btype_ABSOLUTE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Btype_RELATIVE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Ftype_ABSOLUTE_sft_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Itype_ABSOLUTE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Itype_RELATIVE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Jtype_ABSOLUTE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Jtype_RELATIVE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Stype_ABSOLUTE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Utype_ABSOLUTE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_Utype_RELATIVE_imm_relocation(uint32_t input) {
        return input;
        }
        uint32_t RV64IM_hi20(uint32_t symbol) {
        return ((uint32_t) ((symbol) + (2048)) >> (12));
        }
        int16_t RV64IM_lo12(uint32_t symbol) {
        return ((int16_t) symbol);
        }
        """.trim().lines(), output);
  }
}
