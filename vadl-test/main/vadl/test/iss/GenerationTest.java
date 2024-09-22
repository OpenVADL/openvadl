package vadl.test.iss;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass;
import vadl.viam.passes.verification.ViamVerificationPass;

public class GenerationTest extends AbstractTest {

  @Test
  public void test() throws IOException, DuplicatedPassKeyException {
    var config = new GeneralConfiguration(Path.of("build/test-out"), true);
    var setup = setupPassManagerAndRunSpec(
        "sys/risc-v/rv64i.vadl",
        PassOrder.iss(IssConfiguration.from(config))
            .addDump("build/test-out/dump")
    );

    var spec = setup.specification();
  }

}
