package vadl.test.cppCodeGen;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.pass.PassKey;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.DockerExecutionTest;

public class AbstractCppCodeGenTest extends DockerExecutionTest {

  @Override
  public GcbConfiguration getConfiguration(boolean doDump) throws IOException {
    return new GcbConfiguration(super.getConfiguration(doDump));
  }

  public TestSetup runGcbAndCppCodeGen(GcbConfiguration configuration,
                                       String specPath, PassKey until)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpecUntil(specPath,
        PassOrders.gcbAndCppCodeGen(configuration), until);
  }
}
