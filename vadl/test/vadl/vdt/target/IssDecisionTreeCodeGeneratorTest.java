package vadl.vdt.target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.theiling.TheilingDecodeTreeGenerator;

class IssDecisionTreeCodeGeneratorTest extends AbstractDecisionTreeTest {

  @Test
  void testCodeGen_simpleInstructions_succeeds() throws IOException {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "00-"));
    final var tree = new TheilingDecodeTreeGenerator().generate(instructions);

    final var out = new ByteArrayOutputStream();

    /* WHEN */
    try (var generator = new IssDecisionTreeCodeGenerator(out)) {
      generator.generate(tree);
    }

    /* THEN */
    final String result = out.toString();
    System.out.println(result);
  }

  @Test
  void testCodeGene_subsumedInstructions_succeeds() throws IOException {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "10-", "0--"));
    final var tree = new TheilingDecodeTreeGenerator().generate(instructions);

    final var out = new ByteArrayOutputStream();

    /* WHEN */
    try (var generator = new IssDecisionTreeCodeGenerator(out)) {
      generator.generate(tree);
    }

    /* THEN */
    final String result = out.toString();
    System.out.println(result);
  }

}