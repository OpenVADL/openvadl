// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
          assembly I = 'Hello, \\'world\\"'
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    var actualString = (StringLiteral) assembly.expr;
    var expectedString =
        new StringLiteral("'Hello, \\'world\\\"'", SourceLocation.INVALID_SOURCE_LOCATION);
    assertThat(actualString, equalTo(expectedString));
    assertThat(actualString.value, equalTo("Hello, 'world\""));
  }

  @Test
  void parsesDoubleQuotedString() {
    String prog = """
        instruction set architecture TEST = {
          format T : Bits<1> = { a [0..0] }
          instruction I : T = {}
          assembly I = "Hello, \\'world\\""
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    var actualString = (StringLiteral) assembly.expr;
    var expectedString =
        new StringLiteral("\"Hello, \\'world\\\"\"", SourceLocation.INVALID_SOURCE_LOCATION);
    assertThat(actualString, equalTo(expectedString));
    assertThat(actualString.value, equalTo("Hello, 'world\""));
  }

  @Test
  void parsesEscapeSequences() {
    String prog = """
        instruction set architecture TEST = {
          format T : Bits<1> = { a [0..0] }
          instruction I : T = {}
          assembly I = "\\b\\t\\r\\n\\f\\'\\"\\\\\\ud83d\\ude02"
        }
        """;
    var ast = VadlParser.parse(prog);
    var assembly = (AssemblyDefinition) ((InstructionSetDefinition) ast.definitions.get(0))
        .definitions.get(2);
    var actualString = (StringLiteral) assembly.expr;
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
    var group = (GroupedExpr) assembly.expr;
    assertThat(group.expressions.size(), equalTo(4));
  }
}
