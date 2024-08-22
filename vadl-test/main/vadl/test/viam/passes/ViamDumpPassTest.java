package vadl.test.viam.passes;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vadl.dump.HtmlDumpPass;
import vadl.test.AbstractTest;

public class ViamDumpPassTest extends AbstractTest {

  @Test
  void RV3264Im() throws IOException {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");

    var pass = new HtmlDumpPass(new HtmlDumpPass.Config("AstToViam", "build"));
    pass.execute(Map.of(), spec);

  }

}
