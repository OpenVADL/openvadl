package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassResults;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;
import vadl.viam.passes.verification.ViamVerifier;

public class Rv3264imTest extends AbstractTest {

  @Test
  void testRv32im() throws IOException, DuplicatedPassKeyException {
    // runs the general viam optimizations on the rv3264im impl
    var config = new GeneralConfiguration("build/test-out/rv3264im/", true);
    var setup = setupPassManagerAndRunSpec(
        "examples/rv3264im.vadl",
        PassOrder.viam(config)
    );
    var spec = setup.specification();

    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    // Correct program counter
    {
      var pc = findDefinitionByNameIn("RV3264I::PC", rv3264i, Register.Counter.class);
      assertEquals(pc, rv3264i.pc());
    }

  }

}
