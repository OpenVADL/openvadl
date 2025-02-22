package vadl.iss;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public class IssLoweringTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(IssLoweringTest.class);

  // TODO: Remove this (it is just for testing purposes)
  @Test
  void issLoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), true));

    setupPassManagerAndRunSpec("sys/risc-v/rv32im.vadl",
        PassOrders.iss(config)
    );

  }
}
