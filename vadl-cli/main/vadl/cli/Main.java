package vadl.cli;


import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * The VADL CLI entry class.
 */
@Command(mixinStandardHelpOptions = true,
    name = "OpenVADL",
    description = "The OpenVadl CLI tool.",
    subcommands = {CheckCommand.class, IssCommand.class, LcbCommand.class})
public class Main implements Runnable {
  @Override
  public void run() {
    new CommandLine(new Main()).usage(System.out);
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}