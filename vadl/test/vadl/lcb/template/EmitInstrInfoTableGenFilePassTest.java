package vadl.lcb.template;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitInstrInfoTableGenFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException, URISyntaxException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitInstrInfoTableGenFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitInstrInfoTableGenFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    var fs = new File(
        "test/resources/snapshots/rv64im/InstrInfoTableGen.td");
    var expected = FileUtils.readFileToString(fs, "UTF-8");

    Assertions.assertLinesMatch(expected.trim().lines(), output);
  }
}