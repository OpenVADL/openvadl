package vadl.vdt.target.hw;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.theiling.TheilingDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.utils.Instruction;

public class HardwareDecisionTreeCodeGeneratorTest extends AbstractDecisionTreeTest {

  @Test
  void test_generate_riscv64i() throws IOException {

    /* GIVEN */
    final List<Instruction> riscV = parseQemuDecoding("rv64i.decode");
    final Node tree = new TheilingDecodeTreeGenerator().generate(riscV);

    /* WHEN */
    final CharSequence result = new HardwareDecisionTreeCodeGenerator().generate(tree);

    /* THEN */
    System.out.println(result);
  }

}
