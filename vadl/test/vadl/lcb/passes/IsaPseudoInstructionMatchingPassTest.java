package vadl.lcb.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Definition;

public class IsaPseudoInstructionMatchingPassTest extends AbstractLcbTest {

  private static Stream<Arguments> getExpectedMatchings() {
    return Stream.of(
        Arguments.of(List.of("J"), PseudoInstructionLabel.J)
    );
  }

  @ParameterizedTest
  @MethodSource("getExpectedMatchings")
  void shouldFindMatchings(List<String> expectedInstructionName, PseudoInstructionLabel label)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(IsaPseudoInstructionMatchingPass.class.getName()));
    var passManager = setup.passManager();

    // When
    var matchings =
        ((IsaPseudoInstructionMatchingPass.Result) passManager.getPassResults()
            .lastResultOf(IsaPseudoInstructionMatchingPass.class)).labels();

    // Then
    Assertions.assertNotNull(matchings);
    Assertions.assertFalse(matchings.isEmpty());
    var result = matchings.get(label).stream().map(Definition::simpleName).sorted().toList();
    assertEquals(expectedInstructionName.stream().sorted().toList(), result);
  }
}