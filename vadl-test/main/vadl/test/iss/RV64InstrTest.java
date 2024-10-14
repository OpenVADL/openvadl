package vadl.test.iss;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class RV64InstrTest extends QemuIssTest {

  @TestFactory
  Stream<DynamicTest> test() throws IOException {
    var image = generateSimulator("sys/risc-v/rv64i.vadl");

    var testCases = IntStream.range(0, 1).mapToObj(i ->
        new IssTestUtils.TestSpec("ADD_" + i, Map.of("x1", "ffffffffffffffff"),
            """
                addi   x1, x0, -1
                """)
    ).toList();

    return runQemuInstrTests(image, testCases);
  }
}
