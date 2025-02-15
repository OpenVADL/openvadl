package vadl.vdt.target.hw;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.theiling.TheilingDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.utils.Instruction;

public class HardwareDecoderGeneratorTest extends AbstractDecisionTreeTest {

  @Test
  void test_generate_riscv64i_irregular() throws IOException {

    /* GIVEN */
    final List<Instruction> riscV = parseQemuDecoding("rv64i.decode");
    final Node tree = new TheilingDecodeTreeGenerator().generate(riscV);

    /* WHEN */
    final CharSequence result = new HardwareIrregularDecoderGenerator().generate(tree);

    /* THEN */
    System.out.println(result);
  }

  @Test
  void test_generate_riscv64i_regular() throws IOException {

    /* GIVEN */
    final List<Instruction> riscV = parseQemuDecoding("rv64i.decode");
    final Node tree = new TheilingDecodeTreeGenerator().generate(riscV);

    /* WHEN */
    final CharSequence result = new HardwareRegularDecoderGenerator().generate(tree);

    /* THEN */
    System.out.println(result);
  }

}
