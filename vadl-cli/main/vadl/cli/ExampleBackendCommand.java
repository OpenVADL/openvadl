package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.error.DiagnosticPrinter;

/**
 * An example of how to implement a command for a backend that relies on invoking the frontend.
 */
@Command(
    name = "example",
    description = "A example backend",
    mixinStandardHelpOptions = true
)
public class ExampleBackendCommand extends FrontendCommand implements Callable<Integer> {

  // TODO: Remove this option, it's just a showcase that the backend can have options the frontend
  // doesn't have.
  @Option(names = {"--print-secret"}, scope = INHERIT,
      description = "A secret option just in the example.")
  boolean printSecret;

  private final CliState state = CliState.getInstance();

  // TODO: Remove Supression
  @SuppressWarnings("UnusedVariable")
  private void command() {
    var viam = parseToVIAM();

    // TODO: Implement your logic here and delete the lines below
    if (printSecret) {
      System.out.println("I will never tell you my secret, I won't even dump it.");

      state.dumpFile(output, "secret.txt",
          "Huch! You found my gold \uD83C\uDFC5\uD83D\uDCB0");
    }
  }

  @Override
  public Integer call() {
    var returnVal = 0;
    try {
      command();
    } catch (Diagnostic d) {
      new DiagnosticPrinter().print(d);
      returnVal = 1;
    } catch (DiagnosticList d) {
      new DiagnosticPrinter().print(d);
      returnVal = 1;
    }

    state.printDumpedFiles();
    return returnVal;
  }
}
