package vadl.test.lcb.template.include.llvm.BinaryFormat.ELFRelocs;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitTargetElfRelocsDefFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(
            vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(
                vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef ELF_RELOC
        #error "ELF_RELOC must be defined"
        #endif
                
        ELF_RELOC(R_rv64im_NONE, 0)
        ELF_RELOC(R_rv64im_32, 1)
        ELF_RELOC(R_rv64im_64, 2)
                
        ELF_RELOC(R_RV64IM_Btype_ABSOLUTE_imm, 3)
        ELF_RELOC(R_RV64IM_Btype_RELATIVE_imm, 4)
        ELF_RELOC(R_RV64IM_Ftype_ABSOLUTE_sft, 5)
        ELF_RELOC(R_RV64IM_lo12, 6)
        ELF_RELOC(R_RV64IM_Itype_ABSOLUTE_imm, 7)
        ELF_RELOC(R_RV64IM_Itype_RELATIVE_imm, 8)
        ELF_RELOC(R_RV64IM_Jtype_ABSOLUTE_imm, 9)
        ELF_RELOC(R_RV64IM_Jtype_RELATIVE_imm, 10)
        ELF_RELOC(R_RV64IM_Stype_ABSOLUTE_imm, 11)
        ELF_RELOC(R_RV64IM_hi20, 12)
        ELF_RELOC(R_RV64IM_Utype_ABSOLUTE_imm, 13)
        ELF_RELOC(R_RV64IM_Utype_RELATIVE_imm, 14)
        """.trim().lines(), output);
  }
}
