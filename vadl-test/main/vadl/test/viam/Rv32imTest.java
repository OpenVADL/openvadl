package vadl.test.viam;

import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;

public class Rv32imTest extends AbstractTest {

  @Test
  void testRv32im() {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
  }

}
