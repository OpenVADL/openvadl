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


import static org.assertj.core.api.Assertions.assertThat;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import vadl.error.Diagnostic;

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
    assertThat(regFile.annotations).size().isEqualTo(1);
    assertThat(regFile.annotations.getFirst().values.getFirst()).isInstanceOf(CallIndexExpr.class);
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


  private String zeroExtendTest(String annotation, @Nullable String otherDefs) {
    return """
        instruction set architecture TEST =
        {
          %s
        
          %s
          register X : Bits<5> -> Bits<32>
        }
        """.formatted(otherDefs == null ? "" : otherDefs, annotation);
  }

  @Test
  void zeroAnnoInvalidStructure() {
    var prog = zeroExtendTest("[ zero : 2 + 3 - 1]", null);
    var ast = VadlParser.parse(prog);
    var typechecker = new TypeChecker();
    var diag = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    assertThat(diag)
        .hasMessageContaining("Zero annotation must be of form [ zero : <register>( <expr> ) ]");
  }

  @Test
  void zeroAnnoInvalidTarget() {
    var prog = zeroExtendTest("[ zero : M(1)]", "memory M: Bits<5> -> Bits<64>");
    var ast = VadlParser.parse(prog);
    var typechecker = new TypeChecker();
    var diag = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    assertThat(diag)
        .hasMessageContaining("Zero annotation target must be the annotated register.");
  }

  @Test
  void zeroAnnoInvalidTarget2() {
    var prog = zeroExtendTest("[ zero : Y(1)]", "register Y: Bits<5> -> Bits<64>");
    var ast = VadlParser.parse(prog);
    var typechecker = new TypeChecker();
    var diag = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    assertThat(diag)
        .hasMessageContaining("Zero annotation target must be the annotated register.");
  }

  @Test
  void zeroAnnoInvalidArgumentNumber() {
    var prog = zeroExtendTest("[ zero : X(1)(2) ]", null);
    var ast = VadlParser.parse(prog);
    var typechecker = new TypeChecker();
    var diag = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    assertThat(diag)
        .hasMessageContaining("Exactly one register index was expected, but found 2.");
  }

  @Test
  void zeroAnnoInvalidArgument() {
    var prog = zeroExtendTest("[ zero : X(Y) ]", "register Y: Bits<5>");
    var ast = VadlParser.parse(prog);
    var typechecker = new TypeChecker();
    var diag = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    assertThat(diag)
        .hasMessageContaining("Index must be a constant expression.");
  }
}
