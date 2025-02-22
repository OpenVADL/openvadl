package vadl.viam;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.viam.passes.verification.ViamVerifier;

public class RelocationTest extends AbstractTest {

  @Test
  void testRelocation() throws IOException {
    var spec = runAndGetViamSpecification("unit/relocation/valid_relocations.vadl");
    ViamVerifier.verifyAllIn(spec);

    var test = TestUtils.findDefinitionByNameIn("Test", spec, InstructionSetArchitecture.class);
    var r1 = TestUtils.findDefinitionByNameIn("Test::R1", spec, Relocation.class);
    var f1 = TestUtils.findDefinitionByNameIn("Test::F1", spec, Function.class);

    Assertions.assertEquals(1, test.ownRelocations().size());
    Assertions.assertEquals(r1, test.ownRelocations().get(0));
    // relocations should not be added to functions, but hold separately
    Assertions.assertEquals(1, test.ownFunctions().size());
    Assertions.assertEquals(f1, test.ownFunctions().get(0));
  }

}
