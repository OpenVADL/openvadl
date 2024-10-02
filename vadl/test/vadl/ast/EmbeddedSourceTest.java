package vadl.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class EmbeddedSourceTest {

  @Test
  void testEmbeddedSource() {
    var prog = """
        instruction set architecture ISA = {}
        application binary interface ABI for ISA = {}
        micro processor MiP implements ISA with ABI = {
          source TestSource = -<{
            Hello, world!
          }>-
        }
        """;

    var ast = VadlParser.parse(prog);
    var mip = (MicroProcessorDefinition) ast.definitions.get(2);
    var source = (SourceDefinition) mip.definitions.get(0);
    assertThat(source.source.trim(), is("Hello, world!"));
  }
}
