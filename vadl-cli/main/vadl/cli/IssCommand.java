package vadl.cli;

import java.io.IOException;
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

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var issConfig = new IssConfiguration(configuration);
    return PassOrders.iss(issConfig);
  }
}
