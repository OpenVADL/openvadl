package vadl.test.lcb;

import java.io.IOException;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.PassKey;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.cppCodeGen.AbstractCppCodeGenTest;

public abstract class AbstractLcbTest extends AbstractCppCodeGenTest {

  @Override
  public LcbConfiguration getConfiguration(boolean doDump) throws IOException {
    return new LcbConfiguration(super.getConfiguration(doDump),
        new ProcessorName("processorNameValue"));
  }

  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath,
                          PassKey until)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpecUntil(specPath,
        PassOrder.lcb(configuration), until);
  }
}