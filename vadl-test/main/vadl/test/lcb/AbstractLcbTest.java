package vadl.test.lcb;

import java.io.IOException;
import java.util.List;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.cppCodeGen.AbstractCppCodeGenTest;

public abstract class AbstractLcbTest extends AbstractCppCodeGenTest {

  @Override
  public LcbConfiguration getConfiguration(boolean doDump) throws IOException {
    return new LcbConfiguration(super.getConfiguration(doDump),
        new ProcessorName("processorNameValue"));
  }

  /**
   * @deprecated as {@link #setupPassManagerAndRunSpecUntil(String, PassOrder, PassKey)} is also
   *     deprecated.
   */
  @Deprecated
  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath,
                          PassKey until)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpecUntil(specPath,
        PassOrders.lcb(configuration), until);
  }


  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.lcb(configuration));
  }

  /**
   * Inject a temporary {@link Pass} into the {@link PassOrder}.
   *
   * @param after which pass the {@code pass} should be scheduled.
   * @param pass  to be scheduled.
   */
  public record TemporaryTestPassInjection(Class<?> after, Pass pass) {

  }

  /**
   * Sometimes it is required to have additional passes during test execution. However,
   * these passes are not in the default order. With the {@code temporaryPasses} argument,
   * the function caller can specify what passes and when to schedule them.
   *
   * @deprecated as {@link #setupPassManagerAndRunSpecUntil(String, PassOrder, PassKey)} is also
   *     deprecated.
   */
  @Deprecated
  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath,
                          PassKey until,
                          List<TemporaryTestPassInjection> temporaryPasses)
      throws IOException, DuplicatedPassKeyException {
    var passOrder = PassOrders.lcb(configuration);
    for (var tempPass : temporaryPasses) {
      passOrder.addAfterLast(tempPass.after, tempPass.pass);
    }
    return setupPassManagerAndRunSpecUntil(specPath,
        passOrder, until);
  }
}
