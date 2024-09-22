package vadl.test.iss;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;

public class RiscvCompilationTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(RiscvCompilationTest.class);

  @Test
  public void rv64i() throws IOException, DuplicatedPassKeyException {
    var issImage = generateSimulator(
        "sys/risc-v/rv64i.vadl"
    );

    runContainer(issImage, (c) -> {
      c.setCommand("/qemu/build/qemu-system-vadl", "--help");
    }, (c) -> {
    });
  }

}
