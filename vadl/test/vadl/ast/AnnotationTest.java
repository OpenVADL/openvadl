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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnnotationTest {
  @Test
  void constantRegister() {
    var prog = """
        instruction set architecture TEST =
        {
          [ zero : X(0) ]
          register X : Bits<32> -> Bits<5>
        }
        """;
    var ast = VadlParser.parse(prog);
    var isa = (InstructionSetDefinition) ast.definitions.get(0);
    var regFile = isa.definitions.get(0);

    verifyPrettifiedAst(ast);
    assertThat(regFile.annotations.size(), is(1));
    assertThat(regFile.annotations.get(0).values.get(0), is(instanceOf(CallIndexExpr.class)));
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

    Assertions.assertEquals(1, current.annotations.size());
    Assertions.assertEquals(1, current.annotations.get(0).keywords.size());
    Assertions.assertEquals(0, current.annotations.get(0).values.size());

    var nextNext = isa.definitions.get(1);
    Assertions.assertEquals(1, nextNext.annotations.size());
    Assertions.assertEquals(2, nextNext.annotations.get(0).keywords.size());
    Assertions.assertEquals(0, nextNext.annotations.get(0).values.size());
  }
}
