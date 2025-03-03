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

import static vadl.ast.AstTestUtils.assertAstEquality;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.DiagnosticList;

public class ExprTest {
  @Test
  void ifExpressions() {
    var prog = """
        constant a = if (5 > 9) then 32 else 2
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void letExpressions() {
    var prog = """
        constant a = let result = 9 in result - 2
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void bitLengthCannotContainClosingAngleBracket() {
    var prog = """
        constant a: Bits<31 > 2> = 9
        """;

    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void castPrecedence() {
    var prog = """
        constant a = -4 as Bits<3> + 2
        constant b = 5 * 2 + 3 as Bits < 3
        constant c = 9 + 1 as Bits<3>
        """;
    var equiv = """
        constant a = ((-4) as Bits<3>) + 2
        constant b = ((5 * 2) + (3 as Bits)) < 3
        constant c = 9 + (1 as Bits<3>)
        """;

    var ast = VadlParser.parse(prog);
    var expected = VadlParser.parse(equiv);
    assertAstEquality(ast, expected);
  }

  @Test
  void chainedCasts() {
    var prog = """
        constant a = -4 as SInt<3> as Bits
        """;
    var equiv = """
        constant a = ((-4) as SInt<3>) as Bits
        """;

    var ast = VadlParser.parse(prog);
    var expected = VadlParser.parse(equiv);
    assertAstEquality(ast, expected);
  }

  @Test
  void multiRankCast() {
    var prog = """
        constant a = -4 as Bits<3><9> + 2
        constant b = 9 + 1 as Bits<3><4> < 5
        """;
    var equiv = """
        constant a = ((-4) as Bits<3><9>) + 2
        constant b = (9 + (1 as Bits<3><4>)) < 5
        """;

    var ast = VadlParser.parse(prog);
    var expected = VadlParser.parse(equiv);
    assertAstEquality(ast, expected);
  }

  @Test
  void macroCast() {
    var prog = """
        instruction set architecture TEST = {
          model Test (tyId: Id) : IsaDefs = {
            constant a = -4 as $tyId + 2
            constant b = 9 + 1 as $tyId<3><4> < 5
          }
        
          $Test(Bits)
        }
        """;
    var equiv = """
        instruction set architecture TEST = {
          constant a = ((-4) as Bits) + 2
          constant b = (9 + (1 as Bits<3><4>)) < 5
        }
        """;

    var ast = VadlParser.parse(prog);
    var expected = VadlParser.parse(equiv);
    assertAstEquality(ast, expected);
  }

  @Test
  void callExpressions() {
    var prog = """
        instruction set architecture TEST = {
          memory MEM : Bits<8> -> Bits<32>
          constant a = 1
          constant b = 2
          constant simple = MEM
          constant vector = MEM<0x0a>
          constant vectorCall = MEM<0x0a>(9)
          constant simpleCall = MEM(9)
          constant rangeCall = MEM(9..2)
          constant onlyComparisons = a < 2 && b > 2
          constant vectorAddLhs = MEM<0x0a> + 2
          constant vectorAddRhs = 1 + MEM<3>
          constant vectorAddBoth = MEM<9> + MEM<3>
          constant vectorCmpLhs = MEM<3> < a
          constant vectorCmpRhs = a < MEM<3>
          constant vectorCmpBoth = MEM<9> < MEM<3>
        }
        """;

    var ast = VadlParser.parse(prog);
    verifyPrettifiedAst(ast);
  }

  @Test
  void matchExpressions() {
    var prog = """
        constant x = 5
        constant a = match x with 
        { 1 => 0
        , 2, 3 => 4
        , {4, 5} => 6
        , _ => 7
        }
        """;

    var ast = VadlParser.parse(prog);
    verifyPrettifiedAst(ast);
  }
}
