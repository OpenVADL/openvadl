package vadl.test.iss;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Tests the RV64I instructions set.
 */
public class IssRV64MInstrTest extends IssInstrTest {


  @Override
  int getTestPerInstruction() {
    return 50;
  }

  @Override
  String getVadlSpec() {
    return "sys/risc-v/rv64im.vadl";
  }

  AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64ITestBuilder(testNamePrefix + "_" + id);
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> mul() throws IOException {
    return testBinaryRegRegInstruction("mul", "MUL");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> mulh() throws IOException {
    return testBinaryRegRegInstruction("mulh", "MULH");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> mulhu() throws IOException {
    return testBinaryRegRegInstruction("mulhu", "MULHU");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> mulhsu() throws IOException {
    return testBinaryRegRegInstruction("mulhsu", "MULHSU");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> div() throws IOException {
    return testBinaryRegRegInstruction("div", "DIV");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divByZero() throws IOException {
    return testDivRemByCustom(10, "div", BigInteger.ZERO, "DIV_BY_ZERO");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divByMinusOne() throws IOException {
    return testDivRemByCustom(10, "div", BigInteger.ONE.negate(), "DIV_BY_MINUS_ONE");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divu() throws IOException {
    return testBinaryRegRegInstruction("divu", "DIVU");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divuByZero() throws IOException {
    return testDivRemByCustom(10, "divu", BigInteger.ZERO, "DIVU_BY_ZERO");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divuByMinusOne() throws IOException {
    return testDivRemByCustom(10, "divu", BigInteger.ONE.negate(), "DIVU_BY_MINUS_ONE");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> rem() throws IOException {
    return testBinaryRegRegInstruction("rem", "REM");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remByZero() throws IOException {
    return testDivRemByCustom(10, "rem", BigInteger.ZERO, "REM_BY_ZERO");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remByMinusOne() throws IOException {
    return testDivRemByCustom(10, "rem", BigInteger.ONE.negate(), "REM_BY_MINUS_ONE");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remu() throws IOException {
    return testBinaryRegRegInstruction("remu", "REMU");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remuByZero() throws IOException {
    return testDivRemByCustom(10, "remu", BigInteger.ZERO, "REMU_BY_ZERO");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remuByMinusOne() throws IOException {
    return testDivRemByCustom(10, "remu", BigInteger.ONE.negate(), "REMU_BY_MINUS_ONE");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> mulw() throws IOException {
    return testBinaryRegRegInstructionW("mulw", "MULW");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divw() throws IOException {
    return testBinaryRegRegInstructionW("divw", "DIVW");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> divuw() throws IOException {
    return testBinaryRegRegInstructionW("divuw", "DIVUW");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remw() throws IOException {
    return testBinaryRegRegInstructionW("remw", "REMW");
  }

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "test_iss_enabled", matches = "true")
  Stream<DynamicTest> remuw() throws IOException {
    return testBinaryRegRegInstructionW("remuw", "REMUW");
  }

  // Helper methods
  private Stream<DynamicTest> testBinaryRegRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillReg(regSrc1, 64);
      b.fillReg(regSrc2, 64);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return b.toTestSpec(regSrc1, regSrc2, regDest);
    });
  }

  private IssTestUtils.TestSpec customBinaryRegRegInstr(String instr, BigInteger lhs,
                                                        BigInteger rhs,
                                                        AsmTestBuilder b) {
    var regSrc1 = b.anyTempReg().sample();
    var regSrc2 = b.anyTempReg().sample();
    b.fillReg(regSrc1, lhs);
    b.fillReg(regSrc2, rhs);
    var regDest = b.anyTempReg().sample();
    b.add("%s %s, %s, %s", instr, regDest, regSrc1, regSrc2);
    return b.toTestSpec(regSrc1, regSrc2, regDest);
  }

  private Stream<DynamicTest> testDivRemByCustom(int runs, String instr, BigInteger divisor,
                                                 String testPrefix)
      throws IOException {
    return runTestsWith(runs, (i) -> {
      var b = getBuilder(testPrefix, i);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillReg(regSrc1, 64);
      b.fillReg(regSrc2, divisor);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instr, regDest, regSrc1, regSrc2);
      return b.toTestSpec(regSrc1, regSrc2, regDest);
    });
  }

  // Helper for 32-bit wide instructions (sign-extended)
  private Stream<DynamicTest> testBinaryRegRegInstructionW(String instruction,
                                                           String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      // Fill with 32-bit values so we test sign-ext
      b.fillReg(regSrc1, 32);
      b.fillReg(regSrc2, 32);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return b.toTestSpec(regSrc1, regSrc2, regDest);
    });
  }

}
