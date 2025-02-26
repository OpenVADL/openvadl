package vadl.rtl;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public class RtlLoweringTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(RtlLoweringTest.class);

  // TODO remove, not really a test
  @Test
  void rtlLoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new GeneralConfiguration(Path.of("build/test-output"), false);

    setupPassManagerAndRunSpec("sys/risc-v/rv64im.vadl",
        PassOrders.rtl(config)
    );

  }
}
