package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.Function;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Relocation;
import vadl.viam.passes.htmlDump.ViamHtmlDumpPass;
import vadl.viam.passes.verification.ViamVerifier;

public class RelocationTest extends AbstractTest {

  @Test
  void testRelocation() throws IOException {
    var spec = runAndGetViamSpecification("creation/relocation/valid_relocations.vadl");
    ViamVerifier.verifyAllIn(spec);

    new ViamHtmlDumpPass(new ViamHtmlDumpPass.Config("build")).execute(Map.of(), spec);

    var test = findDefinitionByNameIn("Test", spec, InstructionSetArchitecture.class);
    var r1 = findDefinitionByNameIn("Test::R1", spec, Relocation.class);
    var f1 = findDefinitionByNameIn("Test::F1", spec, Function.class);

    assertEquals(1, test.relocations().size());
    assertEquals(r1, test.relocations().get(0));
    assertEquals(1, test.functions().size());
    assertEquals(f1, test.functions().get(0));
  }

}
