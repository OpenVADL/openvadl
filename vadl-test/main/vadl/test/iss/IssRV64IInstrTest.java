package vadl.test.iss;

import static vadl.test.TestUtils.arbitrarySignedInt;
import static vadl.test.TestUtils.arbitraryUnsignedInt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000003"), regSrc, regDest);
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
      return b.toTestSpecCounting(regSrc, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> lui() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64ITestBuilder("LUI_" + id);
      var destReg = b.anyTempReg().sample();
      var value = arbitraryUnsignedInt(20).sample();
      b.add("lui %s, %s", destReg, value);
      return b.toTestSpecCounting(destReg);
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

  @TestFactory
  Stream<DynamicTest> beq() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("BEQ_" + id);

      // Choose arbitrary registers for rs1 and rs2
      var rs1 = b.anyTempReg().sample();
      var rs2 = b.anyTempReg().sample();

      // Randomly decide if rs1 == rs2 or rs1 != rs2
      Boolean equal = Arbitraries.of(true, false).sample();

      // Fill rs1 with a random value
      var val1 = b.fillReg(rs1, 64);

      // Fill rs2 with either the same or a different value
      if (Boolean.TRUE.equals(equal)) {
        // Set rs2 to the same value as rs1
        b.fillReg(rs2, val1);
      } else {
        // Ensure rs2 has a different value from rs1
        var value2 = arbitraryUnsignedInt(64)
            .filter(v -> !v.equals(val1))
            .sample();
        b.fillReg(rs2, value2);
      }

      // Destination register to observe the branch effect
      var destReg = b.anyTempReg().sample();

      // Create unique labels
      String branchLabel = "branch_target_" + id;
      String endLabel = "end_label_" + id;

      // Add the BEQ instruction with the branch label
      b.add("beq %s, %s, %s", rs1, rs2, branchLabel);

      // This instruction will be executed if the branch is not taken
      b.add("addi %s, x0, 1", destReg);

      // Jump over the branch target code
      b.add("j %s", endLabel);

      // Define the branch target label
      b.addLabel(branchLabel);

      // Instruction at the branch target
      b.add("addi %s, x0, 2", destReg);

      // Define the end label
      b.addLabel(endLabel);

      return b.toTestSpec(rs1, rs2, destReg);
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
