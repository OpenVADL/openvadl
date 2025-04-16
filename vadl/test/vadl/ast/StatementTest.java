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

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.utils.SourceLocation;

public class StatementTest {

  @Test
  void parseLetStatement() {
    var prog = """
        instruction set architecture ISA = {
          program counter PC : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
          instruction BEQ : Btype = {
            let next = PC + 4 in
              PC := next
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void parseLetStatementWithMultipleVariables() {
    var prog = """
        instruction set architecture ISA = {
          program counter PC : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
          instruction BEQ : Btype = {
            // Contrived example, would use VADL::adds in the real world
            let next, status = PC + 4 in
              if status = 0 then
                PC := next
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void parseIfStatement() {
    var prog = """
        instruction set architecture ISA = {
          constant a = 9
          constant b = 2
          program counter PC : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
          instruction BEQ : Btype = {
            if a > b then
              PC := PC + 4
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void parseIfElseStatement() {
    var prog = """
        instruction set architecture ISA = {
          constant a = 9
          constant b = 2
          program counter PC : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
          instruction BEQ : Btype = {
            if a > b then
              PC := PC + 4
            else
              PC := 0
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void parseNestedIfElseStatement() {
    var prog = """
        instruction set architecture ISA = {
          format F : Bits<32> = {
            bits [31..0]
          }
          instruction TEST : F = {
            if 3 > 4 then
              if 9 < 2 then {}
              else {}
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var expectedAst = new Ast();
    var loc = SourceLocation.INVALID_SOURCE_LOCATION;
    List<Definition> definitions = List.of(
        new FormatDefinition(
            new Identifier("F", loc),
            new TypeLiteral(new Identifier("Bits", loc),
                List.of(List.of(new IntegerLiteral("32", loc))), loc),
            List.of(new RangeFormatField(
                new Identifier("bits", loc),
                List.of(new RangeExpr(new IntegerLiteral("31", loc),
                    new IntegerLiteral("0", loc))),
                null
            )),
            List.of(),
            loc
        ),
        new InstructionDefinition(
            new Identifier("TEST", loc),
            new Identifier("F", loc),
            new BlockStatement(loc).add(
                new IfStatement(
                    new BinaryExpr(
                        new IntegerLiteral("3", loc),
                        new BinOp(Operator.Greater, loc),
                        new IntegerLiteral("4", loc)),
                    new IfStatement(
                        new BinaryExpr(
                            new IntegerLiteral("9", loc),
                            new BinOp(Operator.Less, loc),
                            new IntegerLiteral("2", loc)),
                        new BlockStatement(loc),
                        new BlockStatement(loc),
                        loc
                    ),
                    null,
                    loc
                )),
            loc
        )
    );
    expectedAst.definitions.add(new InstructionSetDefinition(
        new Identifier("ISA", loc),
        null,
        definitions,
        loc
    ));
    verifyPrettifiedAst(ast);
    Assertions.assertEquals(expectedAst.prettyPrintToString(), ast.prettyPrintToString());
    Assertions.assertEquals(expectedAst, ast);
  }

  @Test
  void parseLockStatement() {
    var prog = """
        instruction set architecture ISA = {
          memory MEM  : Bits<32> -> Bits<32>
          format Btype : Bits<32> = { bits [31..0] }
          instruction INC : Btype = {
            lock MEM(0x1000'0000) in {
              MEM(0x1000'0000) := MEM(0x1000'0000) + 0x1
            }
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

}
