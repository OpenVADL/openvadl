package vadl.test.lcb.passes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.pass.PassKey;
import vadl.test.AbstractTest;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.passes.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class IsaMatchingPassTest extends AbstractTest {

  IsaMatchingPass pass = new IsaMatchingPass();

  private static Stream<Arguments> getExpectedMatchings() {
    return Stream.of(
        Arguments.of(List.of("ADD"), InstructionLabel.ADD_64),
        Arguments.of(List.of("ADDI"), InstructionLabel.ADDI_64),
        Arguments.of(List.of("BEQ"), InstructionLabel.BEQ),
        Arguments.of(List.of("BNE"), InstructionLabel.BNEQ),
        Arguments.of(List.of("BGE", "BGEU"), InstructionLabel.BGEQ),
        Arguments.of(List.of("BLT", "BLTU"), InstructionLabel.BLTH),
        Arguments.of(List.of("AND", "ANDI"), InstructionLabel.AND),
        Arguments.of(List.of("SUB", "SUBW"), InstructionLabel.SUB),
        Arguments.of(List.of("OR", "ORI"), InstructionLabel.OR),
        Arguments.of(List.of("XOR"), InstructionLabel.XOR),
        Arguments.of(List.of("XORI"), InstructionLabel.XORI),
        Arguments.of(List.of("MUL", "MULW", "MULHSU", "MULH"), InstructionLabel.MUL),
        Arguments.of(List.of("DIV", "DIVW"), InstructionLabel.SDIV),
        Arguments.of(List.of("DIVU", "DIVUW"), InstructionLabel.UDIV),
        Arguments.of(List.of("REMU", "REMUW"), InstructionLabel.UMOD),
        Arguments.of(List.of("REM", "REMW"), InstructionLabel.SMOD),
        Arguments.of(List.of("SLT", "SLTU", "SLTI", "SLTIU"), InstructionLabel.LT),
        Arguments.of(List.of("LB", "LBU", "LD", "LH", "LHU", "LW", "LWU"),
            InstructionLabel.LOAD_MEM),
        Arguments.of(List.of("SB", "SD", "SH", "SW"), InstructionLabel.STORE_MEM),
        Arguments.of(List.of("JALR"), InstructionLabel.JALR)
    );
  }

  @ParameterizedTest
  @MethodSource("getExpectedMatchings")
  void shouldFindMatchings(List<String> expectedInstructionName, InstructionLabel label)
      throws IOException {
    // Given
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var passResults = new HashMap<PassKey, Object>();

    new TypeCastEliminationPass().execute(passResults, spec);
    passResults.put(new PassKey("FunctionInlinerPass"),
        new FunctionInlinerPass().execute(passResults, spec));

    // When
    HashMap<InstructionLabel, List<Instruction>> matchings =
        (HashMap<InstructionLabel, List<Instruction>>) pass.execute(passResults, spec);

    // Then
    Assertions.assertNotNull(matchings);
    Assertions.assertFalse(matchings.isEmpty());
    var result = matchings.get(label).stream().map(Definition::name).sorted().toList();
    assertEquals(expectedInstructionName.stream().sorted().toList(), result);
  }
}