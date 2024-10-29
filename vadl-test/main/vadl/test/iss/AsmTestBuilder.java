package vadl.test.iss;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitrary;

public abstract class AsmTestBuilder {

  private final String testId;
  private final List<String> instructions = new ArrayList<>();

  public AsmTestBuilder(String testId) {
    this.testId = testId;
  }


  abstract Arbitrary<String> anyTempReg();

  abstract Arbitrary<String> anyReg();

  abstract BigInteger fillReg(String reg, int size);

  @FormatMethod
  AsmTestBuilder add(String instr, Object... args) {
    instructions.add(String.format(instr, args));
    return this;
  }

  protected abstract String referenceQemuExec();

  public String toAsmString() {
    return String.join("\n", instructions);
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
