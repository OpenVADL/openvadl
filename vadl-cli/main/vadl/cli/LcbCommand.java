package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the iss subcommand.
 */
@Command(
    name = "lcb",
    description = "Generate the LCB (LLVM Compiler Backend)",
    mixinStandardHelpOptions = true
)
public class LcbCommand extends BaseCommand {

  @LazyInit
  @Option(names = {"-p",
      "--process"}, required = true, scope = INHERIT, description = "Processor Name")
  String processorName;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var issConfig = new LcbConfiguration(configuration, new ProcessorName(processorName));
    return PassOrders.lcb(issConfig);
  }
}
