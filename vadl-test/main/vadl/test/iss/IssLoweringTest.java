package vadl.test.iss;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;

public class IssLoweringTest extends AbstractTest {

  @Test
  void issLoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), false));
    setupPassManagerAndRunSpec("sys/risc-v/rv64i.vadl",
        PassOrders.iss(config)
    );
  }
}
