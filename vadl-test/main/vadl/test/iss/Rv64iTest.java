package vadl.test.iss;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.shaded.com.google.common.collect.Streams;

public class Rv64iTest extends QemuExecutionTest {

  @TestFactory
  Stream<DynamicTest> test() throws IOException {
    var spec = runAndGetViamSpecification("sys/risc-v/rv64i.vadl");
    var image = getQemuTestImage(Path.of("./"), spec);

    var testCases = IntStream.range(0, 1).mapToObj(i ->
        new TestSpec("ADD_" + i, Map.of("a0", "abcdef9876543210"),
            """
                li a0, 0xABCDEF9876543210
                li a1, 0x10000000
                li a2, 0xFFFFFF0001000FFF
                """)
    ).toList();

    return runQemuInstrTests(image, testCases);
  }

}
