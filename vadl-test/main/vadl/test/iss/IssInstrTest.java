package vadl.test.iss;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;

public abstract class IssInstrTest extends QemuIssTest {

  abstract int getTestPerInstruction();

  abstract String getVadlSpec();

  abstract AsmTestBuilder getBuilder(String testNamePrefix, int id);

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    return runTestsWith(getTestPerInstruction(), generators);
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      int runs,
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    var image = generateIssSimulator(getVadlSpec());
    var testCases = Stream.of(generators)
        .flatMap(genFunc -> IntStream.range(0, runs)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }

}
