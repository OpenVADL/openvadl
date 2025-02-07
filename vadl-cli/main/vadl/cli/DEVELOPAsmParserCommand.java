package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import picocli.CommandLine;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * TEMPORARY command to develop the asm parser generator.
 */
@CommandLine.Command(
    name = "asm",
    mixinStandardHelpOptions = true
)
public class DEVELOPAsmParserCommand extends BaseCommand {
  @LazyInit
  @CommandLine.Option(names = {"-p",
      "--process"}, required = true, scope = INHERIT, description = "Processor Name")
  String processorName;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var lcbConfig = new LcbConfiguration(configuration, new ProcessorName(processorName));
    return PassOrders.temporaryAsmParserGen(lcbConfig);
  }
}
