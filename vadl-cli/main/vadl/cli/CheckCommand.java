package vadl.cli;

import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the check subcommand.
 */
@Command(
    name = "check",
    description = "Verify the correctness of a VADL file without generating anything.",
    mixinStandardHelpOptions = true
)
public class CheckCommand extends BaseCommand implements Callable<Integer> {

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    return PassOrders.viam(configuration);
  }
}
