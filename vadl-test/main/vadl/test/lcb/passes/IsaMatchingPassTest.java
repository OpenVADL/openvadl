package vadl.test.lcb.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.pass.PassKey;
import vadl.test.AbstractTest;
import vadl.viam.Instruction;
import vadl.viam.passes.FunctionInlinerPass;

public class IsaMatchingPassTest extends AbstractTest {

  IsaMatchingPass pass = new IsaMatchingPass();

  private static Stream<Arguments> getExpectedMatchings() {
    return Stream.of(
        Arguments.of("ADD", InstructionLabel.ADD_64),
        Arguments.of("ADDI", InstructionLabel.ADDI_64)
    );
  }

  @ParameterizedTest
  @MethodSource("getExpectedMatchings")
  void shouldFindMatchings(String expectedInstructionName, InstructionLabel label)
      throws IOException {
    // Given
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var passResults = new HashMap<PassKey, Object>();

    new FunctionInlinerPass().execute(passResults, spec);

    // When
    HashMap<InstructionLabel, Instruction> matchings =
        (HashMap<InstructionLabel, Instruction>) pass.execute(passResults, spec);

    // Then
    assertFalse(matchings.isEmpty());
    assertEquals(expectedInstructionName, matchings.get(label).name());
  }
}
