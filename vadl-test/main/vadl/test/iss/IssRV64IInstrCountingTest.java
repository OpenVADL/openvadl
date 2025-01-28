package vadl.test.iss;

import static vadl.test.TestUtils.arbitrarySignedInt;
import static vadl.test.TestUtils.arbitraryUnsignedInt;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the RV64I instruction but also counts the instructions executed.
 * It does so by checking insn_count QEMU register.
 * Keep in mind that the test framework has a special harness that hides additional
 * instructions so the expected insn_count is always larger than the instructions actually
 * executed.
 * See vadl-test/main/resources/scripts/iss_qemu/test_case_executer_v1.py
 */
public class IssRV64IInstrCountingTest extends QemuIssTest {
  private static final int TESTS_PER_INSTRUCTION = 1;
  private static final Logger log = LoggerFactory.getLogger(IssRV64IInstrCountingTest.class);

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> addi_count_ins() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("ADDI_" + id);
      var aImm = arbitrarySignedInt(12).sample();
      var regSrc = b.anyTempReg().sample();
      b.add("addi %s, x0, %s", regSrc, aImm);

      var bImm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("addi %s, %s, %s", regDest, regSrc, bImm);
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000003"), regSrc, regDest);
    });
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> addiw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("ADDIW_" + id);
      var aImm = arbitrarySignedInt(12).sample();
      var regSrc = b.anyTempReg().sample();
      b.add("addiw %s, x0, %s", regSrc, aImm);

      var bImm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("addiw %s, %s, %s", regDest, regSrc, bImm);
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000003"), regSrc, regDest);
    });
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> lui() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64ITestBuilder("LUI_" + id);
      var destReg = b.anyTempReg().sample();
      var value = arbitraryUnsignedInt(20).sample();
      b.add("lui %s, %s", destReg, value);
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000002"), destReg);
    });
  }

  @SafeVarargs
  private Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    var image = generateCasSimulator("sys/risc-v/rv64i.vadl");
    var testCases = Stream.of(generators)
        .flatMap(genFunc -> IntStream.range(0, TESTS_PER_INSTRUCTION)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }
}
