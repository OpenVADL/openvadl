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
        uint32_t RV3264I_Btype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Ftype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Itype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Jtype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Rtype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Stype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Utype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Btype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Itype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Jtype_generated(uint32_t input) {
        return input;
        }
        uint32_t RV3264I_Utype_generated(uint32_t input) {
        return input;
        }
        """.trim().lines(), output);
  }
}
