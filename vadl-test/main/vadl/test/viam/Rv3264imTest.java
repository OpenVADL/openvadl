package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import vadl.pass.PassResults;
import vadl.test.AbstractTest;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;
import vadl.viam.passes.verification.ViamVerifier;

public class Rv3264imTest extends AbstractTest {

  @Test
  void testRv32im() throws IOException {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    new TypeCastEliminationPass(getConfiguration(false)).execute(PassResults.empty(), spec);
    try {
      ViamVerifier.verifyAllIn(spec);
    } catch (ViamGraphError e) {
      System.out.println(Objects.requireNonNull(e.graph()).dotGraph());
      throw e;
    }

    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    // Correct program counter
    {
      var pc = findDefinitionByNameIn("RV3264I::PC", rv3264i, Register.Counter.class);
      assertEquals(pc, rv3264i.pc());
    }

  }

}
