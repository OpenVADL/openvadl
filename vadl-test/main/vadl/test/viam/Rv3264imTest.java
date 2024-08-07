package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;

public class Rv3264imTest extends AbstractTest {

  @Test
  void testRv32im() {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    // Correct program counter
    {
      var pc = findDefinitionByNameIn("RV3264I::PC", rv3264i, Register.Counter.class);
      assertEquals(pc, rv3264i.pc());
    }

  }

}
