package vadl.test.viam.passes;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.passes.htmlDump.ViamHtmlDumpPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class ViamDumpPassTest extends AbstractTest {

  @Test
  void RV3264Im() throws IOException {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");

    var pass = new ViamHtmlDumpPass(new ViamHtmlDumpPass.Config("build/dump"));
    pass.execute(Map.of(), spec);

  }

}
