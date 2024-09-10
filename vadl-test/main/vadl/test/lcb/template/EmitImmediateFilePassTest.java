package vadl.test.lcb.template;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class EmitImmediateFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var testSetup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(EmitImmediateFilePass.class.getName()));
    var passManager = testSetup.passManager();
    var spec = testSetup.specification();

    // When
    var template =
        new EmitImmediateFilePass(getConfiguration(false));

    // When
    var result = template.renderToString(passManager.getPassResults(), spec);
    var trimmed = result.trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        
        """.trim().lines(), output);
  }
}
