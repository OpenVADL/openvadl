package vadl.cli;


import picocli.CommandLine;

/**
 * The VADL CLI entry class.
 */
@CommandLine.Command(mixinStandardHelpOptions = true,
    name = "OpenVADL",
    description = "The OpenVadl CLI tool.",
    subcommands = {FrontendCommand.class, ExampleBackendCommand.class})
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