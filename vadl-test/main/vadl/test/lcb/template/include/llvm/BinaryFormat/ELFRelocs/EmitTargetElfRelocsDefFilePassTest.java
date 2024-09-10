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
                
        ELF_RELOC(R_processorNameValue_ABS_RV3264I_Btype_RV3264I_Btype_imm, 3)
        ELF_RELOC(R_processorNameValue_ABS_RV3264I_Ftype_RV3264I_Ftype_sft, 4)
        ELF_RELOC(R_processorNameValue_ABS_RV3264I_Itype_RV3264I_Itype_imm, 5)
        ELF_RELOC(R_processorNameValue_ABS_RV3264I_Jtype_RV3264I_Jtype_imm, 6)
        ELF_RELOC(R_processorNameValue_ABS_RV3264I_Stype_RV3264I_Stype_imm, 7)
        ELF_RELOC(R_processorNameValue_ABS_RV3264I_Utype_RV3264I_Utype_imm, 8)
        ELF_RELOC(R_processorNameValue_REL_RV3264I_Btype_RV3264I_Btype_imm, 9)
        ELF_RELOC(R_processorNameValue_REL_RV3264I_Itype_RV3264I_Itype_imm, 10)
        ELF_RELOC(R_processorNameValue_REL_RV3264I_Jtype_RV3264I_Jtype_imm, 11)
        ELF_RELOC(R_processorNameValue_REL_RV3264I_Utype_RV3264I_Utype_imm, 12)
        """.trim().lines(), output);
  }
}
