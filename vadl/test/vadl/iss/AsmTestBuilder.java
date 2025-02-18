package vadl.iss;

import static vadl.TestUtils.arbitraryBetween;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitrary;

public abstract class AsmTestBuilder {

  private final String testId;
  private final List<String> instructions = new ArrayList<>();

  public AsmTestBuilder(String testId) {
    this.testId = testId;
  }


  abstract Arbitrary<String> anyTempReg();

  abstract Arbitrary<String> anyReg();

  abstract BigInteger fillReg(String reg, BigInteger value);

  BigInteger fillReg(String reg, BigInteger min, BigInteger max) {
    var val = arbitraryBetween(min, max).sample();
    return fillReg(reg, val);
  }

  BigInteger fillReg(String reg, int size) {
    return fillReg(reg,
        BigInteger.valueOf(-2).pow(size - 1),
        BigInteger.valueOf(2)
            .pow(size - 1)
            .subtract(BigInteger.ONE)
    );
  }

  void addLabel(String label) {
    add("%s:", label);
  }

  @FormatMethod
  AsmTestBuilder add(String instr, Object... args) {
    instructions.add(String.format(instr, args));
    return this;
  }

  protected abstract String referenceQemuExec();

  public String toAsmString() {
    return String.join("\n", instructions);
  }

  IssTestUtils.TestSpec toTestSpecWithSpecialRegs(Map<String, String> map, String... regsOfInterest) {
    return new IssTestUtils.TestSpec(
        testId,
        map,
        toAsmString(),
        referenceQemuExec(),
        List.of(regsOfInterest)
    );
  }

  IssTestUtils.TestSpec toTestSpec(
      String... regsOfInterest
  ) {
    return new IssTestUtils.TestSpec(
        testId,
        Map.of(),
        toAsmString(),
        referenceQemuExec(),
        List.of(regsOfInterest)
    );
  }

  IssTestUtils.TestSpec toTestSpec(
      List<String> regsOfInterest
  ) {
    return new IssTestUtils.TestSpec(
        testId,
        Map.of(),
        toAsmString(),
        referenceQemuExec(),
        regsOfInterest
    );
  }


}
