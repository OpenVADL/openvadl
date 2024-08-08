package vadl.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import vadl.utils.SourceLocation;

public class StringTest {
  @Test
  void parsesSingleQuotedString() {
    String prog = """
        instruction set architecture TEST = {
          format T : Bits<1> = { a [0..0] }
          instruction I : T = {}
          assembly I = ('Hello, \\'world\\"')
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    var actualString = (StringLiteral) assembly.segments.get(0);
    var expectedString =
        new StringLiteral("'Hello, \\'world\\\"'", SourceLocation.INVALID_SOURCE_LOCATION);
    assertThat(assembly.segments.size(), equalTo(1));
    assertThat(actualString, equalTo(expectedString));
    assertThat(actualString.value, equalTo("Hello, 'world\""));
  }

  @Test
  void parsesDoubleQuotedString() {
    String prog = """
        instruction set architecture TEST = {
          format T : Bits<1> = { a [0..0] }
          instruction I : T = {}
          assembly I = ("Hello, \\'world\\"")
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    var actualString = (StringLiteral) assembly.segments.get(0);
    var expectedString =
        new StringLiteral("\"Hello, \\'world\\\"\"", SourceLocation.INVALID_SOURCE_LOCATION);
    assertThat(assembly.segments.size(), equalTo(1));
    assertThat(actualString, equalTo(expectedString));
    assertThat(actualString.value, equalTo("Hello, 'world\""));
  }

  @Test
  void parsesEscapeSequences() {
    String prog = """
        instruction set architecture TEST = {
          format T : Bits<1> = { a [0..0] }
          instruction I : T = {}
          assembly I = ("\\b\\t\\r\\n\\f\\'\\"\\\\\\ud83d\\ude02")
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    var actualString = (StringLiteral) assembly.segments.get(0);
    assertThat(actualString.value, equalTo("\b\t\r\n\f'\"\\😂"));
  }

  @Test
  void onlyParsesUntilClosingQuote() {
    String prog = """
        instruction set architecture TEST = {
          format T : Bits<1> = { a [0..0] }
          instruction I : T = {}
          assembly I = ("a", "b", 'c', 'd')
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    assertThat(assembly.segments.size(), equalTo(4));
  }
}
