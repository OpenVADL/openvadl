package vadl.ast;

import static vadl.ast.AstTestUtils.loadVadlFiles;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class AstDumpTests {

  /**
   * Compares the computed dump of files in "test/resources/dumps/*.vadl" with the corresponding
   * expected dump in "test/resources/dumps/*.dump".
   * If a change in the code changes the dump representation, the expected dump file needs to be
   * adapted as well.
   * In this case, a file "*.dump.actual" will be generated.
   * If that file is the correct dump, simply replace the old dump with it.
   */
  @TestFactory
  Stream<DynamicTest> astDumpTests() throws URISyntaxException, IOException {
      return loadVadlFiles("dumps").stream()
          .filter(path -> path.getFileName().toString().endsWith(".vadl"))
          .map(path -> DynamicTest.dynamicTest("Parse " + path.getFileName(),
              () -> assertDumpEquality(path)));
  }

  private void assertDumpEquality(Path vadlPath) throws IOException {
    var ast = VadlParser.parse(vadlPath.toAbsolutePath());
    verifyPrettifiedAst(ast);

    var actualDump = new AstDumper().dump(ast);
    var expectedDumpPath = dumpPath(vadlPath);
    if (!Files.isRegularFile(expectedDumpPath)) {
      writeDump(actualDumpPath(vadlPath), actualDump);
      Assertions.fail("File " + expectedDumpPath + " does not exist");
    }
    var expectedDump = Files.readString(expectedDumpPath).replaceAll("\r\n", "\n");
    
    if (actualDump.equals(expectedDump)) {
      // Clean up any ".actual" files we might have left behind the last time this test failed
      Files.deleteIfExists(actualDumpPath(vadlPath));
    } else {
      writeDump(actualDumpPath(vadlPath), actualDump);
    }

    Assertions.assertEquals(expectedDump, actualDump,
        "Expected dump:\n" + expectedDump + "\nActual dump:\n" + actualDump);
  }

  private void writeDump(Path dumpPath, String dump) throws IOException {
    Files.writeString(dumpPath, dump);
  }

  private Path dumpPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".dump");
    return vadlPath.resolveSibling(path);
  }

  private Path actualDumpPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".dump.actual");
    return vadlPath.resolveSibling(path);
  }
}
