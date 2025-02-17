package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.Function;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Relocation;
import vadl.viam.passes.verification.ViamVerifier;

public class RelocationTest extends AbstractTest {

  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @Test
  void testRelocation() throws IOException {
    var spec = runAndGetViamSpecification("unit/relocation/valid_relocations.vadl");
    ViamVerifier.verifyAllIn(spec);

    var test = findDefinitionByNameIn("Test", spec, InstructionSetArchitecture.class);
    var r1 = findDefinitionByNameIn("Test::R1", spec, Relocation.class);
    var f1 = findDefinitionByNameIn("Test::F1", spec, Function.class);

    assertEquals(1, test.ownRelocations().size());
    assertEquals(r1, test.ownRelocations().get(0));
    // relocations should not be added to functions, but hold separately
    assertEquals(1, test.ownFunctions().size());
    assertEquals(f1, test.ownFunctions().get(0));
  }

}
