package vadl.test.iss;

import static vadl.test.TestUtils.arbitrarySignedInt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.types.DataType;
import vadl.viam.Constant;

/**
 * Tests the RV64I instructions set
 */
public class IssRV64IInstrTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(IssRV64IInstrTest.class);

  // Collection of all test generators
  private final Stream<Function<Integer, IssTestUtils.TestSpec>> TEST_GENERATORS = Stream.of(
      IssRV64IInstrTest::gen12BitAddiTest
  );

  @TestFactory
  Stream<DynamicTest> test() throws IOException {
    var image = generateSimulator("sys/risc-v/rv64i.vadl");

    var testNumberPerGenerator = 200;
    var testCases = TEST_GENERATORS
        .flatMap(genFunc -> IntStream.range(0, testNumberPerGenerator)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }

  /**
   * Generates a random test that tests the ADDI instruction with 12-bit immediate values.
   *
   * @return test spec to run on QEMU ISS instance.
   */
  private static IssTestUtils.TestSpec gen12BitAddiTest(int testId) {
    var immSrcReg = arbitrarySignedInt(12).sample();
    var immArg = arbitrarySignedInt(12).sample();

    var regSrc = arbitraryRegNonSignal().filter(r -> !r.equals("x0")).sample();
    var regDest = arbitraryRegNonSignal().filter(r -> !r.equals("x0")).sample();

    var instr = new StringBuilder()
        .append("addi   %s, x0, %s\n".formatted(regSrc, immSrcReg))
        .append("addi   %s, %s, %s\n".formatted(regDest, regSrc, immArg));

    var arg1Const = Constant.Value.of(immSrcReg.intValue(), DataType.signedInt(12))
        .signExtend(DataType.bits(64));
    var arg2Const = Constant.Value.of(immArg.intValue(), DataType.bits(12))
        .signExtend(DataType.bits(64));

    var resultConst = arg1Const.add(arg2Const, false).firstValue();

    var regTest = new HashMap<String, String>();
    regTest.put(regDest, resultConst.hexadecimal(""));
    if (!Objects.equals(regSrc, regDest)) {
      regTest.put(regSrc, arg1Const.hexadecimal(""));
    }

    return new IssTestUtils.TestSpec(
        "ADDI_" + testId,
        regTest,
        instr.toString());
  }

  private static Arbitrary<String> arbitraryReg(String... except) {
    return arbitraryReg().filter(r -> Stream.of(except).noneMatch(r::equals));
  }

  private static Arbitrary<String> arbitraryReg() {
    return Arbitraries.of(IntStream.range(0, 32).mapToObj(i -> "x" + i).toList());
  }

  private static Arbitrary<String> arbitraryRegNonSignal() {
    return arbitraryReg("x6");
  }

}
