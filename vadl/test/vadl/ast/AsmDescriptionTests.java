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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.DiagnosticList;

public class AsmDescriptionTests {

  private String inputWrappedByValidAsmDescription(String input) {
    return """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            %s
          }
        """.formatted(input);
  }

  @Test
  void asmDescriptionWithModifier() {
    var prog = """
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI
            }
        
            grammar = {
              A : "B" ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithMultipleModifiers() {
    var prog = """
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
            relocation LO ( symbol : Bits <32> ) -> Bits <12> =   symbol         & 0xFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI,
              "lo" -> ISA::LO
            }
        
            grammar = {
              A : "B" ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionDuplicateModifierString() {
    var prog = """
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
            relocation LO ( symbol : Bits <32> ) -> Bits <12> =   symbol         & 0xFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI,
              "hi" -> ISA::LO
            }
        
            grammar = {
              A : "B" ;
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithEmptyModifiers() {
    var prog = """
          modifiers = {
          }
        
          grammar = {
            A : "B" ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(
        inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithDirective() {
    var prog = """
          directives = {
            "dir1" -> ALIGN_POW2
          }
        
          grammar = {
            A : "B" ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithMultipleDirectives() {
    var prog = """
          directives = {
            "dir1" -> ALIGN_POW2,
            "dir2" -> BYTE4
          }
        
          grammar = {
            A : "B" ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionDuplicateDirectiveString() {
    var prog = """
          directives = {
            "dir1" -> ALIGN_POW2,
            "dir1" -> BYTE4
          }
        
          grammar = {
            A : "B" ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithEmptyDirectives() {
    var prog = """
          directives = {
          }
        
          grammar = {
            A : "B" ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(
        inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithModifiersAndDirectives() {
    var prog = """
          instruction set architecture ISA = {
            relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
            relocation LO ( symbol : Bits <32> ) -> Bits <12> =   symbol         & 0xFFF
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI,
              "lo" -> ISA::LO
            }
        
            directives = {
              "dir1" -> BYTE4,
              "dir2" -> ASCII
            }
        
            grammar = {
              A : "B" ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithFunctions() {
    var prog = """
          function minus (x : SInt<64>) -> SInt<64> = -x
          function minus32 (x : SInt<32>) -> SInt<32> = -x
        
          grammar = {
            A : a = minus32<Integer> ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithInvalidFunctionDefinition() {
    var prog = """
          function minus (x) -> SInt<64> = -x   // argument without type
        
          grammar = {
            A : a = minus<Integer> ;
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)),
        "Invalid function definition");
  }

  @Test
  void asmDescriptionWithConstantAndFunction() {
    var prog = """
          constant one = 1
        
          function minusOne (x : SInt<64>) -> SInt<64> = x - one
          function minusOne32 (x : SInt<32>) -> SInt<32> = x - one
        
          grammar = {
            A : a = minusOne32<Integer> ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithUsingAndFunction() {
    var prog = """
          using char = Bits<8>
        
          function minusOne (x : char) -> char = -x
        
          grammar = {
            A : a = minusOne<Integer> ;
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(inputWrappedByValidAsmDescription(prog)));
  }

  @Test
  void asmDescriptionWithFunctionFromISA() {
    var prog = """
          instruction set architecture ISA = {
            function minusOne (x : char) -> char = -x
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            grammar = {
              A : a = minusOne<Integer> ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionWithFunctionCallAsFunctionCallArgument() {
    var prog = """
          instruction set architecture ISA = {
            function minusOne (x : SInt<64>) -> SInt<64> = x - 1
          }
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            grammar = {
              A : a = minusOne<minusOne<Integer>> ;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void asmDescriptionNotAllowedFormatDefinition() {
    var prog = """
          format F : Bits<16> =
           { rs2 : RegIndex
           , rs1 : RegIndex
           , rd : RegIndex
           , op : Bits<4>
           }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)),
        "Format definition not allowed in assembly description");
  }

  @Test
  void asmDescriptionNotAllowedModelDefinition() {
    var prog = """
          model CreateInstr ( instr : Id , fmt: Id , behavior : Stats) : IsaDefs = {
            instruction $instr : $fmt = {
              $behavior
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)),
        "Model definition not allowed in assembly description");
  }

  @Test
  void asmDescriptionReferencesUnknownAbi() {
    var prog = """
          assembly description AD for ABI = {}
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void asmModifiersReferenceUnknownRelocations() {
    var prog = """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
          assembly description AD for ABI = {
            modifiers = {
              "hi" -> ISA::HI
            }
          }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
  }

  @Test
  void allSortsOfNestedFunctionCalls() {
    var prog = """
          instruction set architecture ISA = {
            function oneArg (x : SInt<64>) -> SInt<64> = 1
            function twoArgs (x : SInt<64>,y : SInt<64>) -> SInt<64> = 1
            function threeArgs (x : SInt<64>,y : SInt<64>, z : SInt<64>) -> SInt<64> = 1
          }
        
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            grammar = {
              A : a = oneArg<Integer>;
              B : a = oneArg<oneArg<Integer>>;
              C : a = oneArg<oneArg<oneArg<Integer>>>;
              D : a = oneArg<oneArg<oneArg<oneArg<Integer>>>>;
              E : a = oneArg<oneArg<oneArg<oneArg<oneArg<Integer>>>>>;
              F : a = oneArg<oneArg<oneArg<oneArg<oneArg<oneArg<Integer>>>>>>;
        
              G : a = twoArgs<oneArg<oneArg<Integer>>,oneArg<Integer>>;
              H : a = twoArgs<oneArg<Integer>,Integer>;
              I : a = twoArgs<oneArg<oneArg<oneArg<Integer>>>,oneArg<Integer>>;
              J : a = twoArgs<oneArg<oneArg<oneArg<oneArg<oneArg<oneArg<Integer>>>>>>,oneArg<oneArg<oneArg<oneArg<oneArg<oneArg<Integer>>>>>>>;
        
              K : a = threeArgs<Integer,Integer,Integer>;
              L : a = twoArgs<threeArgs<Integer,Integer,Integer>,threeArgs<threeArgs<Integer,Integer,Integer>,Integer,threeArgs<Integer,Integer,Integer>>>;
              M : a = threeArgs<twoArgs<oneArg<Integer>,Integer>,twoArgs<oneArg<Integer>,Integer>,twoArgs<oneArg<Integer>,Integer>>;
        
              N : a = threeArgs<Integer<>,Integer<>,Integer<>>;
              O : a = threeArgs<oneArg<Integer<>>,Integer<>,Integer<>>;
              P : a = threeArgs<Integer,Integer,oneArg<Integer<>>>;
        
              Q : a = threeArgs<oneArg<oneArg<Integer<>>>,Integer,oneArg<oneArg<Integer<>>>>;
            }
          }
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }
}
