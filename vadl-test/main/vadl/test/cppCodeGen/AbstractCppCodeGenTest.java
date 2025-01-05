package vadl.test.cppCodeGen;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.pass.PassKey;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.DockerExecutionTest;

public class AbstractCppCodeGenTest extends DockerExecutionTest {

  @Override
  public GcbConfiguration getConfiguration(boolean doDump) {
    return new GcbConfiguration(super.getConfiguration(doDump));
  }

  public record TestCase(String testName, String code) {

  }

  /**
   * Runs gcb passorder.
   */
  public TestSetup runGcbAndCppCodeGen(GcbConfiguration configuration,
                                       String specPath)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath, PassOrders.gcbAndCppCodeGen(configuration));
  }
}
