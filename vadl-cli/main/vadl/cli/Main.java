package vadl.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * The VADL CLI entry class.
 */
@Command(name = "OpenVADL", mixinStandardHelpOptions = true,
    subcommands = { AstCommands.DumpAstCommand.class, AstCommands.ExpandAstCommand.class })
public class Main implements Runnable {

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}