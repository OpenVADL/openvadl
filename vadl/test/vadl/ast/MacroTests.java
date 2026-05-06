// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.error.DiagnosticList;
import vadl.utils.SingleFileVirtualFileSystem;

public class MacroTests {

  @Test
  void singleExpressionTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
        
        constant n = $example()
        """;
    var prog2 = "constant n = 1 + 2";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void singleExpressionWithoutParenthesisTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
        
        constant n = $example
        """;
    var prog2 = "constant n = 1 + 2";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void binaryOrderInMacroTest() {
    var prog1 = """
        model concreteOps(): Ex = {
          1 + 2 * 3 = 8 && 7 + 9 > 10
        }
        
        model placeholderOps(op1: BinOp, op2: BinOp, op3: BinOp, op4: BinOp, op5: BinOp, op6: BinOp)
        : Ex = {
          1 $op1 2 $op2 3 $op3 8 $op4 7 $op5 9 $op6 10
        }
        
        constant a = $concreteOps()
        constant b = $placeholderOps(+ ; * ; = ; && ; + ; >)
        """;
    var prog2 = """
        constant a = ((1 + (2 * 3)) = 8) && ((7 + 9) > 10)
        constant b = ((1 + (2 * 3)) = 8) && ((7 + 9) > 10)
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void groupingOutsideMacroTest() {
    var prog1 = """
        model example() : Ex =  {
          1 + 2
        }
        
        constant n = 3 * $example()
        """;
    var prog2 = "constant n = 3 * (1 + 2)";

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void macroWithUnusedArguments() {
    var prog1 = """
        model example(first: Int, second: Ex) : Ex =  {
          1 + 2
        }
        
        constant n = 3 * $example(3 ; 5)
        """;
    var prog2 = "constant n = 3 * (1 + 2)";
    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }




  @Test
  void passIdAsParameter() {
    var prog1 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          model test(opName: Id, instrFormat : Id) : IsaDefs = {
            instruction $opName : $instrFormat = {
              A := bits
            }
          }
        
          $test(SET ; F)
        }
        """;

    var prog2 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          instruction SET : F = {
            A := bits
          }
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }


  @Test
  void macroProducingStatements() {
    var prog1 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          register B : Bits<32>
          register C : Bits<32>
          model test(targetReg: Id, sourceReg1: Id, sourceReg2: Id) : Stats = {
            $targetReg := $sourceReg1 + $sourceReg2
          }
          instruction ADD : F = {
            $test(A ; B ; C)
          }
        }
        """;

    var prog2 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          register B : Bits<32>
          register C : Bits<32>
          instruction ADD : F = {
            A := B + C
          }
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void canHandleMultipleInvocations() {
    var prog1 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          format G : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          model test(opName: Id, instrFormat : Id) : IsaDefs = {
            instruction $opName : $instrFormat = {
              A := bits
            }
          }
        
          $test(SET_F ; F)
          $test(SET_G ; G)
        }
        """;

    var prog2 = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          format G : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          instruction SET_F : F = {
            A := bits
          }
          instruction SET_G : G = {
            A := bits
          }
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void attachesCorrectExpandedFrom() {
    // Since the macroExpander is responsible for attaching the correct expandedFrom locations
    // to each locations these tests are here.

    var prog = """
        model x() : Ex = {
          1 + doesnotExists
        }
        
        constant a = $x()
        """;

    var path = Paths.get("hardcoded");
    var fileSystem = new SingleFileVirtualFileSystem(prog, path);
    var exception = Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(path, fileSystem));
    var location = exception.items.get(0).multiLocation.primaryLocation().location();
    Assertions.assertEquals(1, location.expandedFromStack().size());
    Assertions.assertEquals(5, location.expandedFromStack().getFirst().begin().line());
  }

  @Test
  void attachesCorrectNestedExpandedFrom() {
    // Since the macroExpander is responsible for attaching the correct expandedFrom locations
    // to each locations these tests are here.

    var prog = """
        model inner(): Ex = {
            2 + xyz
        }
        
        model outer(): Ex = {
            1 + $inner()
        }
        
        constant name = $outer()
        """;

    var exception = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
    var location = exception.items.get(0).multiLocation.primaryLocation().location();
    Assertions.assertEquals(2, location.expandedFromStack().size());
    var firstExpanded = location.expandedFromStack().getFirst();
    var secondExpanded = location.expandedFromStack().get(1);
    Assertions.assertEquals(6, firstExpanded.begin().line());
    Assertions.assertEquals(9, secondExpanded.begin().line());
  }

  @Test
  void macroIsaWithInheritance() {
    var prog1 = """
        model BName(): Id = { Base }
        
        instruction set architecture $BName() = {
          model Test(): Id = { Test }
        }
        
        instruction set architecture Sub extending $BName() = {
          constant $Test() = 3
        }
        """;
    var prog2 = """
        instruction set architecture Base = { }
        
        instruction set architecture Sub extending Base = {
          constant Test = 3
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void macroIsaWithInheritanceRecursive() {
    var prog1 = """
        instruction set architecture Base0 = {
          model Test(): Id = { Test }
        }
        instruction set architecture Base1 extending Base0 = { }
        instruction set architecture Sub extending Base1 = {
          constant $Test() = 3
        }
        """;
    var prog2 = """
        instruction set architecture Base0 = { }
        instruction set architecture Base1 extending Base0 = { }
        instruction set architecture Sub extending Base1 = {
          constant Test = 3
        }
        """;

    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }


  @Test
  void asIdTest() {
    // There once was a time where record return types couldn't be parsed.
    var prog1 = """
        constant AsId("one") = 1
        constant AsId("th", "ree") = 3
        constant AsId(max, count) = 42
        constant AsId(open, "vadl") = 2024
        """;
    var prog2 = """
        constant one = 1
        constant three = 3
        constant maxcount = 42
        constant openvadl = 2024
        """;
    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @Test
  void asStrTest() {
    // There once was a time where record return types couldn't be parsed.
    var prog1 = """
        function one -> String = AsStr(one)
        function three -> String = AsStr(th, ree)
        function maxcount -> String = AsStr("max", "count")
        function openvadl -> String = AsStr("open", vadl)
        """;
    var prog2 = """
        function one -> String = "one"
        function three -> String = "three"
        function maxcount -> String = "maxcount"
        function openvadl -> String = "openvadl"
        """;
    assertAstEquality(VadlParser.parse(prog1), VadlParser.parse(prog2));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "7", "_x", "encoding"})
  void invalidIdentifierAsId(String string) {
    var prog = """
        constant AsId("%s") = 6
        """.formatted(string);
    var diagnostics = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
    var diagnostic = diagnostics.items.getFirst();
    Assertions.assertTrue(
        diagnostic.reason.contains("Invalid") && diagnostic.reason.contains("Identifier"),
        "Reason was: `%s`".formatted(diagnostic.reason));
  }
}
