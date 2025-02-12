package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the iss subcommand.
 */
@Command(
    name = "iss",
    description = "Generate the ISS (Instruction Set Simulator)",
    mixinStandardHelpOptions = true
)
public class IssCommand extends BaseCommand {

  @CommandLine.Option(names = {"--dry-run"},
      scope = INHERIT,
      description = "Don't emit generated files.")
  boolean dryRun;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var issConfig = new IssConfiguration(configuration);
    issConfig.setDryRun(dryRun);
    return PassOrders.iss(issConfig);
  }
}
