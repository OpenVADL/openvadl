package vadl.test.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lld.ELF.Arch.EmitLldManualEncodingHeaderFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

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
        uint32_t RV3264I_BtypeRV3264I_Btype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (4095)) << (7));
        }
        uint32_t RV3264I_FtypeRV3264I_Ftype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        }
        uint32_t RV3264I_FtypeRV3264I_Ftype_sft(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (63)) << (20));
        }
        uint32_t RV3264I_ItypeRV3264I_Itype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (4095)) << (20));
        }
        uint32_t RV3264I_ItypeRV3264I_Itype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        }
        uint32_t RV3264I_JtypeRV3264I_Jtype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (1048575)) << (12));
        }
        uint32_t RV3264I_JtypeRV3264I_Jtype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        }
        uint32_t RV3264I_RtypeRV3264I_Rtype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        }
        uint32_t RV3264I_StypeRV3264I_Stype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (4095)) << (7));
        }
        uint32_t RV3264I_UtypeRV3264I_Utype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (1048575)) << (12));
        }
        uint32_t RV3264I_UtypeRV3264I_Utype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        }
        uint32_t RV3264I_BtypeRV3264I_Btype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (4095)) << (7));
        }
        uint32_t RV3264I_ItypeRV3264I_Itype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        }
        uint32_t RV3264I_JtypeRV3264I_Jtype_imm(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (1048575)) << (12));
        }
        uint32_t RV3264I_UtypeRV3264I_Utype_rd(uint32_t instWord,uint32_t newValue) {
        return ((instWord) & (2)) | (((newValue) & (31)) << (7));
        } 
        """.trim().lines(), output);
  }
}
