package vadl.ast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;

public class AstTestUtils {
  static void verifyPrettifiedAst(Ast ast) {
    var progPretty = ast.prettyPrint();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty),
        "Cannot parse prettified input \n" + progPretty);
    assertAstEquality(astPretty, ast);
  }

  static void assertAstEquality(Ast actual, Ast expected) {
    if (!actual.equals(expected)) {
      var prettyActual = actual.prettyPrint();
      var prettyExpected = expected.prettyPrint();
      Assertions.assertEquals(actual, expected,
          "Expected: " + prettyExpected + "Actual: " + prettyActual);
    }
  }

  static List<Path> loadVadlFiles(String directory) throws URISyntaxException, IOException {
    var dir = Objects.requireNonNull(AstTestUtils.class.getClassLoader().getResource(directory));
    var sourceDir = Path.of(dir.toURI()).toAbsolutePath().toString()
        .replace("/build/resources/test", "/test/resources");
    try (Stream<Path> files = Files.list(Path.of(sourceDir))) {
      return files.toList();
    }
  }
}
