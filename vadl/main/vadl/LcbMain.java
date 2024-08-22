package vadl;

import java.io.IOException;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.EmitLcbMakeFilePass;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * The main vadl program, providing a cli.
 */
public class LcbMain {

  /**
   * The main vadl method, providing a cli.
   *
   * @param args are teh command line arguments.
   */
  public static void main(String[] args) throws IOException, DuplicatedPassKeyException {
    var passManager = new PassManager();
    var configuration = new LcbConfiguration("output");
    passManager.add(new PassKey("lcbMakefile"),
        new EmitLcbMakeFilePass(configuration, new ProcessorName("CPU")));

    passManager.run(
        new Specification(new Identifier("test", SourceLocation.INVALID_SOURCE_LOCATION)));
  }
}
