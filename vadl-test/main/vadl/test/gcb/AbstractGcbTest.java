package vadl.test.gcb;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.pass.PassKey;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.cppCodeGen.AbstractCppCodeGenTest;

public abstract class AbstractGcbTest extends AbstractCppCodeGenTest {

  @Override
  public GcbConfiguration getConfiguration(boolean doDump) throws IOException {
    return new GcbConfiguration(super.getConfiguration(doDump));
  }

  public TestSetup runGcb(GcbConfiguration configuration,
                          String specPath,
                          PassKey until)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpecUntil(specPath,
        PassOrders.gcbAndCppCodeGen(configuration), until);
  }
}