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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.ast.AstTestUtils.assertAstEquality;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;

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
  void invalidMacroReturnType() {
    var prog = """
        model example() : Int =  {
           1 + 2
        }
        
        constant n = 3 * $example()
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
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
  void invalidSyntaxTypeReturn() {
    var prog = """
        model example(arg: Ex) : DoesntExist =  {
           1 + 2
        }
        
        constant n = 3 * $example(3)
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void invalidSyntaxTypeParameter() {
    var prog = """
        model example(arg: DoesntExist) : Ex =  {
           1 + 2
        }
        
        constant n = 3 * $example(3)
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void invalidNotEnoughArguments() {
    var prog = """
        model example(arg: Ex) : Ex =  {
           1 + 2
        }
        
        constant n = 3 * $example()
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void invalidTooManyArguments() {
    var prog = """
        model example(arg: Ex) : Ex =  {
           1 + 2
        }
        
        constant n = 3 * $example(1;2)
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void invalidProvidedArgumentType() {
    var prog = """
        model example(arg: Bool) : Ex =  {
           1 + 2
        }
        
        constant n = 3 * $example(5)
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
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
  void validatesSymbolAvailabilityAtCallSite() {
    var prog = """
        instruction set architecture Test = {
          format F : Bits<32> = { bits [31..0] }
          register A : Bits<32>
          model test(opName: Id, instrFormat : Id) : IsaDefs = {
            instruction $opName : $instrFormat = {
              A := bots // Typo!
            }
          }
        
          $test(SET ; F)
        }
        """;

    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
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

    var exception = Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(prog, URI.create("memory://hardcoded")));
    var location = exception.items.get(0).multiLocation.primaryLocation().location();
    Assertions.assertNotNull(location.expandedFrom());
    Assertions.assertEquals(5, location.expandedFrom().begin().line());
    Assertions.assertNull(location.expandedFrom().expandedFrom());
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
    Assertions.assertNotNull(location.expandedFrom());
    Assertions.assertEquals(6, location.expandedFrom().begin().line());
    Assertions.assertNotNull(location.expandedFrom().expandedFrom());
    Assertions.assertEquals(9, location.expandedFrom().expandedFrom().begin().line());
    Assertions.assertNull(location.expandedFrom().expandedFrom().expandedFrom());
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
  void macroIsaWithInheritanceConflictingDefs() {
    var prog1 = """
        instruction set architecture Base0 = {
          model Test(): Id = { XY }
        }
        instruction set architecture Base1 extending Base0 = {
          model Test(): Id = { XZ } 
        }
        instruction set architecture Sub extending Base1 = {
          constant $Test() = 3
        }
        """;
    var diag = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog1));
    assertThat(diag.items.toArray(Diagnostic[]::new))
        .anySatisfy(d ->
            assertThat(d).hasMessageContaining("Macro name already used: Test"));
  }

  @Test
  void macroIsaWithMultiInheritanceConflictingDefs() {
    var prog1 = """
        instruction set architecture Base0 = {
          model Test(): Id = { XY }
        }
        instruction set architecture Base1 = {
          model Test(): Id = { XZ } 
        }
        instruction set architecture Sub extending Base0, Base1 = {
          constant $Test() = 3
        }
        """;
    var diag = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog1));
    assertThat(diag.items.toArray(Diagnostic[]::new))
        .anySatisfy(d ->
            assertThat(d).hasMessageContaining("Macro name already used: Test"));
  }

  @Test
  void macroInIsaInheritingExtendedRecord() {
    var prog1 = """
        instruction set architecture Base = {
          record Record (id: Id, mnemo: Str, opcode: Ex)
        }
        
        instruction set architecture Final extending Base = {
          model Model (r: Record): Ex = {
            42
          }
        
          constant x = $Model((abc; "xyz"; 1))
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog1));
  }

  @Test
  void macroWithRecordTypes() {
    // There once was a time where record return types couldn't be parsed.
    var prog1 = """
        record InstrWithFunct (id: Id, mnemo: Str, opcode: Ex, funct: Id)
        
        model ExtendInstr (i: InstrWithFunct, ext: Str): InstrWithFunct = {
          (AsId ($i.id,  $ext); $i.mnemo; $i.opcode; $i.funct)
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog1));
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

  @Test
  void assemblyExpandedToMultipleDefinitions() {
    // There once was a bug where the assemblies weren't expanded correctly, and the symboltable
    // threw an error with code like that.
    // https://github.com/OpenVADL/openvadl/issues/304
    var prog = """
        instruction set architecture TEST = {
          format Fa: Bits<32> =
          { field   [31..0]
          }
          format Fb: Bits<64> =
          { field   [63..0]
          }
        
          register    X : Bits<5>   -> Bits<32>
        
          instruction One : Fa = X(0) := 1
          instruction Two : Fb = X(0) := 2
          encoding One = {field = 1}
          encoding Two = {field = 2}
        
          // This line caused the issue before
          assembly One, Two = (mnemonic, " ", decimal(field))
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog));
  }
}


