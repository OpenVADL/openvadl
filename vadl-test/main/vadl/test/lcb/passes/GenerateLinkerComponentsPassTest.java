package vadl.test.lcb.passes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.relocation.model.ElfRelocationName;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class GenerateLinkerComponentsPassTest extends AbstractLcbTest {

  @Test
  public void shouldGenerateRelocations() throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateLinkerComponentsPass.class.getName()));

    // When
    var output =
        (GenerateLinkerComponentsPass.Output) setup.passManager().getPassResults()
            .lastResultOf(GenerateLinkerComponentsPass.class);

    // Then
    var names = List.of(
        "R_RV64IM_lo12_Itype_imm", "R_RV64IM_lo12_Utype_imm", "R_RV64IM_lo12_Stype_imm",
        "R_RV64IM_lo12_Btype_imm", "R_RV64IM_lo12_Jtype_imm", "R_RV64IM_lo12_Ftype_sft",
        "R_RV64IM_hi20_Itype_imm", "R_RV64IM_hi20_Utype_imm", "R_RV64IM_hi20_Stype_imm",
        "R_RV64IM_hi20_Btype_imm", "R_RV64IM_hi20_Jtype_imm", "R_RV64IM_hi20_Ftype_sft",
        "R_RV64IM_Ftype_ABSOLUTE_sft", "R_RV64IM_Btype_ABSOLUTE_imm", "R_RV64IM_Stype_ABSOLUTE_imm",
        "R_RV64IM_Itype_ABSOLUTE_imm", "R_RV64IM_Utype_ABSOLUTE_imm", "R_RV64IM_Jtype_ABSOLUTE_imm",
        "R_RV64IM_Ftype_RELATIVE_sft", "R_RV64IM_Btype_RELATIVE_imm", "R_RV64IM_Stype_RELATIVE_imm",
        "R_RV64IM_Itype_RELATIVE_imm", "R_RV64IM_Utype_RELATIVE_imm", "R_RV64IM_Jtype_RELATIVE_imm"
    );

    assertEquals(names, output.elfRelocations().stream()
        .map(x -> x.elfRelocationName().value())
        .toList());
  }
}
