package vadl.ast;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FrontendIntegrationTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "../sys/risc-v/rv32i.vadl",
      "../sys/risc-v/rv64i.vadl",
      "../sys/risc-v/rv32im.vadl",
      "../sys/risc-v/rv64im.vadl",
  })
  public void testTypecheckRiscvSpec(String filename) {
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(Path.of(filename)),
        "Cannot parse input");
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var lowering = new ViamLowering();
    Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
  }

}
