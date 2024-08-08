package vadl.ast;

import static vadl.ast.AstTestUtils.loadVadlFiles;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class AstMacroTests {

  /**
   * Compares the computed expanded AST of files in "test/resources/macros/*.vadl" with the
   * corresponding expected dump in "test/resources/macros/*.expanded.vadl".
   * If a change in the code changes the AST representation, the expanded AST file needs to be
   * adapted as well.
   * In this case, a file "*.actual.expanded.vadl" will be generated.
   * If that file is a correct AST, simply replace the old AST with it.
   */
  @TestFactory
  Stream<DynamicTest> astMacroTests() throws URISyntaxException, IOException {
    return loadVadlFiles("macros").stream()
        .filter(path -> path.getFileName().toString().endsWith(".vadl")
            && !path.getFileName().toString().endsWith(".expanded.vadl"))
        .map(path -> DynamicTest.dynamicTest("Parse " + path.getFileName(),
            () -> assertAstEquality(path)));
  }

  private void assertAstEquality(Path vadlPath) throws IOException {
    var ast = VadlParser.parse(vadlPath.toAbsolutePath());
    verifyPrettifiedAst(ast);

    var actualExpandedAst = ast.prettyPrint();
    var expectedAstPath = expandedAstPath(vadlPath);
    if (!Files.isRegularFile(expectedAstPath)) {
      writeAst(actualAstPath(vadlPath), actualExpandedAst);
      Assertions.fail("File " + expectedAstPath + " does not exist");
    }
    var expectedAst = Files.readString(expectedAstPath).replaceAll("\r\n", "\n");

    if (actualExpandedAst.equals(expectedAst)) {
      // Clean up any ".actual" files we might have left behind the last time this test failed
      Files.deleteIfExists(actualAstPath(vadlPath));
    } else {
      writeAst(actualAstPath(vadlPath), actualExpandedAst);
    }

    Assertions.assertEquals(expectedAst, actualExpandedAst);
  }

  private void writeAst(Path astPath, String ast) throws IOException {
    Files.writeString(astPath, ast);
  }

  private Path expandedAstPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".expanded.vadl");
    return vadlPath.resolveSibling(path);
  }

  private Path actualAstPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".actual.expanded.vadl");
    return vadlPath.resolveSibling(path);
  }
}
