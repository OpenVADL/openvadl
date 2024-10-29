package vadl.test.iss;

import static vadl.test.TestUtils.arbitrarySignedInt;
import static vadl.test.TestUtils.arbitraryUnsignedInt;

import java.io.IOException;
import java.math.BigInteger;
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
 * Tests the RV64I instructions set.
 */
public class IssRV64IInstrTest extends QemuIssTest {

  private static final int TESTS_PER_INSTRUCTION = 50;
  private static final Logger log = LoggerFactory.getLogger(IssRV64IInstrTest.class);

  @TestFactory
  Stream<DynamicTest> addi() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("ADDI_" + id);
      var aImm = arbitrarySignedInt(12).sample();
      var regSrc = b.anyTempReg().sample();
      b.add("addi %s, x0, %s", regSrc, aImm);

      var bImm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("addi %s, %s, %s", regDest, regSrc, bImm);
      return b.toTestSpec(regSrc, regDest);
    });
  }


  @TestFactory
  Stream<DynamicTest> addiw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("ADDIW_" + id);
      var aImm = arbitrarySignedInt(12).sample();
      var regSrc = b.anyTempReg().sample();
      b.add("addiw %s, x0, %s", regSrc, aImm);

      var bImm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("addiw %s, %s, %s", regDest, regSrc, bImm);
      return b.toTestSpec(regSrc, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> lui() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64ITestBuilder("LUI_" + id);
      var destReg = b.anyTempReg().sample();
      var value = arbitraryUnsignedInt(20).sample();
      b.add("lui %s, %s", destReg, value);
      return b.toTestSpec(destReg);
    });
  }


  @TestFactory
  Stream<DynamicTest> ssli() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64ITestBuilder("SSLI_" + id);
      var srcReg = b.anyTempReg().sample();
      b.fillReg(srcReg, 64);
      var destReg = b.anyTempReg().sample();
      var shiftAmount = arbitraryUnsignedInt(6).sample();
      b.add("slli %s, %s, %s", destReg, srcReg, shiftAmount);
      return b.toTestSpec(srcReg, destReg);
    });
  }

  @TestFactory
  Stream<DynamicTest> lb() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64ITestBuilder("LB_" + id);
      var storeReg = b.anyTempReg().sample();
      b.fillReg(storeReg, 8);
      var addrReg = b.anyTempReg().sample();
      // some memory address in a valid space
      b.fillReg(addrReg, BigInteger.valueOf(0x80000000L), BigInteger.valueOf(0x800F0000L));
      b.add("sb %s, 0(%s)", storeReg, addrReg);

      var loadReg = b.anyTempReg().sample();
      b.add("lb %s, 0(%s)", loadReg, addrReg);
      return b.toTestSpec(storeReg, loadReg, addrReg);
    });
  }

  @SafeVarargs
  private Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    var image = generateSimulator("sys/risc-v/rv64i.vadl");
    var testCases = Stream.of(generators)
        .flatMap(genFunc -> IntStream.range(0, TESTS_PER_INSTRUCTION)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }

}
