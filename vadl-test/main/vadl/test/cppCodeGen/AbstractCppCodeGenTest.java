package vadl.test.cppCodeGen;

import java.io.IOException;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.pass.PassManager;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.DockerExecutionTest;
import vadl.utils.Pair;
import vadl.utils.VADLFileUtils;
import vadl.viam.Specification;

public class AbstractCppCodeGenTest extends DockerExecutionTest {
  /**
   * Returns a dummy configuration.
   */
  public static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("dummyValue");
  }

  public Pair<PassManager, Specification> setupPassManagerAndRunSpec(String path)
      throws IOException, DuplicatedPassKeyException {
    var directory = VADLFileUtils.createTempDirectory("lcbTest");
    var spec = runAndGetViamSpecification(path);

    var passManager = new PassManager();
    passManager.add(
        PassOrder.viamLcb(new LcbConfiguration(directory.toAbsolutePath().toString()),
            new ProcessorName("dummyNamespaceValue")));

    passManager.run(spec);

    return Pair.of(passManager, spec);
  }
}
