package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * The VADL CLI entry class.
 */
@Command(name = "OpenVADL", mixinStandardHelpOptions = true,
    description = "When used without subcommand, checks the given specification for syntax errors",
    subcommands = { AstCommands.DumpAstCommand.class, AstCommands.ExpandAstCommand.class })
public class Main implements Callable<Integer> {

  @Spec
  @LazyInit
  CommandSpec spec;

  @Parameters(description = "Path to the input VADL specification", arity = "0..1")
  @Nullable
  Path input;

  @Option(names = {"-m", "--model"}, scope = INHERIT,
      description = "Override the value of an Id model")
  @Nullable
  Map<String, String> modelOverrides;

  @Option(names = {"-p", "--pass-stats"}, scope = INHERIT,
      description = "Print performance statistics of passes")
  boolean printPassStatistics;

  @Override
  public Integer call() {
    if (input == null) {
      throw new CommandLine.ParameterException(spec.commandLine(), "Missing input parameter");
    }
    var result = AstCommands.checkSyntax(this);
    if (result == 0) {
      System.out.println("No errors found");
    }
    return result;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}