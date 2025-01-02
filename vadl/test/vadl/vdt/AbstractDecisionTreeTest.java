package vadl.vdt;

import java.util.ArrayList;
import java.util.List;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;
import vadl.viam.Identifier;
import vadl.viam.graph.Graph;

public class AbstractDecisionTreeTest {

  protected List<Instruction> createInsns(List<String> instructions) {
    final List<Instruction> result = new ArrayList<>();

    int i = 0;
    for (String insn : instructions) {
      final String name = "insn_" + i++;

      result.add(new Instruction() {
        @Override
        public vadl.viam.Instruction source() {
          var id = Identifier.noLocation(name);
          var behaviour = new Graph("mock");
          return new vadl.viam.Instruction(id, behaviour, null, null);
        }

        @Override
        public int width() {
          return insn.length();
        }

        @Override
        public BitPattern pattern() {
          final PBit[] bits = new PBit[insn.length()];
          for (int i = 0; i < insn.length(); i++) {
            bits[i] = new PBit(insn.charAt(i) == '1' ? PBit.Value.ONE
                : (insn.charAt(i) == '0' ? PBit.Value.ZERO : PBit.Value.DONT_CARE));
          }
          return new BitPattern(bits);
        }

        @Override
        public String toString() {
          return "Instruction{" + pattern() + "}";
        }
      });
    }
    return result;
  }

}
