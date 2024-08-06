package vadl.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Test;

public class AnnotationTest {
  @Test
  void constantRegister() {
    var prog = """
        instruction set architecture TEST =
        {
          [ X(0) = 0 ]
          register file X : Bits<32> -> Bits<5>
        }
        """;
    var ast = VadlParser.parse(prog);
    var isa = (InstructionSetDefinition) ast.definitions.get(0);
    var regFile = isa.definitions.get(0);

    verifyPrettifiedAst(ast);
    assertThat(regFile.annotations.annotations().size(), is(1));
    assertThat(regFile.annotations.annotations().get(0).expr(), is(instanceOf(BinaryExpr.class)));
  }

  @Test
  void keywordAnnotations() {
    var prog = """
        instruction set architecture TEST =
        {
          [ current ]
          program counter CURRENT : Bits<32>
          [ next next ]
          program counter NEXTNEXT : Bits<32>
        }
        """;
    var ast = VadlParser.parse(prog);
    verifyPrettifiedAst(ast);

    var isa = (InstructionSetDefinition) ast.definitions.get(0);
    var current = isa.definitions.get(0);

    assertThat(current.annotations.annotations().size(), is(1));
    assertThat(current.annotations.annotations().get(0).expr(), is(instanceOf(CallExpr.class)));
    assertThat(current.annotations.annotations().get(0).property(), is(nullValue()));

    var nextNext = isa.definitions.get(1);
    assertThat(nextNext.annotations.annotations().size(), is(1));
    assertThat(nextNext.annotations.annotations().get(0).expr(), is(instanceOf(CallExpr.class)));
    assertThat(nextNext.annotations.annotations().get(0).property(),
        is(instanceOf(Identifier.class)));
  }
}
