package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the rtl subcommand.
 */
@Command(
    name = "rtl",
    description = "Generate the RTL description (Chisel)",
    mixinStandardHelpOptions = true
)
public class RtlCommand extends BaseCommand {

  @CommandLine.Option(names = {"--dry-run"},
      scope = INHERIT,
      description = "Don't emit generated files.")
  boolean dryRun;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    configuration.setDryRun(dryRun);
    return PassOrders.rtl(configuration);
  }
}
