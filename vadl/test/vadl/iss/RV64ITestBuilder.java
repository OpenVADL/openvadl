package vadl.iss;

import java.math.BigInteger;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;

public class RV64ITestBuilder extends AsmTestBuilder {

  public RV64ITestBuilder(String testId) {
    super(testId);
  }


  @Override
  BigInteger fillReg(String reg, BigInteger value) {
    add("li %s, %s", reg, value);
    return value;
  }

  @Override
  protected String referenceQemuExec() {
    return "qemu-system-riscv64";
  }

  @Override
  Arbitrary<String> anyTempReg() {
    return Arbitraries.of("x5", "x7", "x28", "x29", "x30", "x31");
  }

  @Override
  Arbitrary<String> anyReg() {
    return Arbitraries.of(IntStream.range(0, 32).mapToObj(i -> "x" + i).toList());
  }
}
