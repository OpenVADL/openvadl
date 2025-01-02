package vadl.vdt.impl.theiling;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.vdt.model.Node;
import vadl.vdt.target.DtStatisticsCalculator;
import vadl.vdt.target.InMemoryDecoder;
import vadl.vdt.target.dto.DecisionTreeStatistics;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;

class TheilingDecodeTreeGeneratorTest {

  @Test
  void testGenerate_simpleInstructions_succeeds() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "00-"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final InMemoryDecoder decoder = new InMemoryDecoder(dt);

    assertDecision(decoder, "100", "1--");
    assertDecision(decoder, "110", "1--");
    assertDecision(decoder, "101", "1--");
    assertDecision(decoder, "111", "1--");

    assertDecision(decoder, "010", "01-");
    assertDecision(decoder, "011", "01-");

    assertDecision(decoder, "001", "00-");
    assertDecision(decoder, "000", "00-");
  }

  @Test
  void testGenerate_subsumedInstructions_succeeds() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "10-", "0--"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final InMemoryDecoder decoder = new InMemoryDecoder(dt);

    assertDecision(decoder, "100", "10-");
    assertDecision(decoder, "101", "10-");

    assertDecision(decoder, "110", "1--");
    assertDecision(decoder, "111", "1--");

    assertDecision(decoder, "000", "0--");
    assertDecision(decoder, "001", "0--");
    assertDecision(decoder, "010", "0--");
    assertDecision(decoder, "011", "0--");
  }

  @Test
  void testGenerate_statistics_1() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "00-"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DtStatisticsCalculator calculator = new DtStatisticsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(5, stats.getNumberOfNodes());
    Assertions.assertEquals(3, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(2, stats.getMaxDepth());
    Assertions.assertEquals(1.67, Math.round(stats.getAvgDepth() * 100) / 100.0);
  }

  @Test
  void testGenerate_statistics_2() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "000", "001"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DtStatisticsCalculator calculator = new DtStatisticsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(7, stats.getNumberOfNodes());
    Assertions.assertEquals(4, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(3, stats.getMaxDepth());
    Assertions.assertEquals(2.25, stats.getAvgDepth());
  }

  @Test
  void testGenerate_statistics_3() {

    /* GIVEN */
    final var instructions = createInsns(
        List.of("100", "101", "110", "111", "010", "011", "000", "001"));

    /* WHEN */
    final Node dt = new TheilingDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DtStatisticsCalculator calculator = new DtStatisticsCalculator();
    final DecisionTreeStatistics stats = calculator.calculate(dt);

    Assertions.assertEquals(9, stats.getNumberOfNodes());
    Assertions.assertEquals(8, stats.getNumberOfLeafNodes());
    Assertions.assertEquals(1, stats.getMinDepth());
    Assertions.assertEquals(1, stats.getMaxDepth());
    Assertions.assertEquals(1, stats.getAvgDepth());
  }

  private void assertDecision(InMemoryDecoder decoder, String insn, String expected) {
    Instruction decision = decoder.decide(BitVector.fromString(insn, insn.length()));
    Assertions.assertNotNull(decision);
    Assertions.assertEquals(expected, decision.pattern().toString());
  }

  private List<Instruction> createInsns(List<String> instructions) {
    final List<Instruction> result = new ArrayList<>();
    for (String insn : instructions) {
      result.add(new Instruction() {
        @Override
        public vadl.viam.Instruction source() {
          // TODO: implement
          return null;
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