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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.DiagnosticList;

/**
 * Checks if the name resolution in the parser works as expected.
 */
public class NameResolutionTest {
  @Test
  void resolveSingleConstant() {
    var prog = "constant a = 13";
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveTwoConstant() {
    var prog = """
          constant a = 13
          constant b = 13
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveTwoOverlappingConstant() {
    var prog = """
          constant a = 13
          constant a = 13
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw name conflict");
    Assertions.assertEquals(1, thrown.items.size());
  }

  @Test
  void resolveUndefinedVariable() {
    var prog = """
          constant a = b
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw unresolved variable");
    Assertions.assertEquals(1, thrown.items.size());
  }

  @Test
  void resolvePreviouslyDefinedVariable() {
    var prog = """
          constant a = 13
          constant b = a
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveInTheFutureDefinedVariable() {
    var prog = """
          constant b = a
          constant a = 13
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  // @Test
  void resolveCyclicDefinedVariable() {
    var prog = """
          constant a = a
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw unresolved variable");
    Assertions.assertEquals(1, thrown.items.size());
  }

  // @Test
  void resolveTwoCyclicDefinedVariables() {
    var prog = """
          constant a = b
          constant b = a
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw unresolved variable");
    Assertions.assertEquals(1, thrown.items.size());
  }

  @Test
  void resolveTwoMemoryDefinitions() {
    var prog = """
        instruction set architecture FLO = {
          memory MEM: Bits<32> -> Bits<8>
          memory SideMem: Bits<6> -> Bits<2>
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void resolveTwoOverlappingMemoryDefinitions() {
    var prog = """
        instruction set architecture FLO = {
          memory MEM: Bits<32> -> Bits<8>
          memory MEM: Bits<6> -> Bits<2>
        }
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw name conflict");
    Assertions.assertEquals(1, thrown.items.size());
  }

  @Test
  void resolveTwoOverlappingRegisterDefinitions() {
    var prog = """
        instruction set architecture FLO = {
          register X : Bits<5>
          register X : Bits<2>
        }
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw name conflict");
    Assertions.assertEquals(1, thrown.items.size());
  }

  @Test
  void resolveTwoOverlappingRegisterFileDefinitions() {
    var prog = """
        instruction set architecture FLO = {
          register file X : Bits<5> -> Bits<32>
          register file X : Bits<2> -> Bits<4>
        }
        """;
    var thrown = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Expected to throw name conflict");
    Assertions.assertEquals(1, thrown.items.size());
  }

  @Test
  void registersAvailableInInstruction() {
    var prog = """
        instruction set architecture ISA = {
          register X : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
          instruction BEQ : Btype = {
            X := 0
          }
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void formatFieldsAvailableInInstruction() {
    var prog = """
        instruction set architecture ISA = {
          format Btype : Bits<32> = {
            a [31..16],
            b [15..8],
            c [7..0]
          }
          instruction BEQ : Btype = {
            a := b * c
          }
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void nestedFormatFieldsResolve() {
    var prog = """
        instruction set architecture ISA = {
          format Byte : Bits<8> = {
            bits: Bits<8>
          }
        
          format Short : Bits<16> = {
            byte1: Byte,
            byte2: Byte
          }
        
          format Btype : Bits<32> = {
            a [31..0]
          }
        
          register X : Short
        
          instruction BEQ : Btype = {
            X.byte1 := X.byte2.bits + X.byte1.bits
          }
        }
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  // @Test
  // TODO Reenable. Unsure how the symbol table should check if all subcalls are valid
  void rejectsMistypedNestedFieldAccess() {
    var prog = """
        instruction set architecture ISA = {
          format Byte : Bits<8> = {
            bits: Bits<8>
          }
        
          format Short : Bits<16> = {
            byte1: Byte,
            byte2: Byte
          }
        
          format Btype : Bits<32> = {
            a [31..0]
          }
        
          register X : Short
        
          instruction BEQ : Btype = {
            X.byte1 := X.byte2.bats + X.byte1.bits
          }
        }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Should reject typos");
  }

  @Test
  void typeSizeDependsOnConstant() {
    var prog = """
        constant a = 3
        constant b: SInt<a> = 1
        """;
    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void invalidTypeSizeNameDoesNotExist() {
    var prog = """
        constant b: SInt<a> = 1
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Should reject typos");
  }

  @Test
  void invalidTypeNameInUsing() {
    var prog = """
        instruction set architecture ISA = {
          using Word        = SInt<XSize>
        }
        """;
    Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog),
        "Should reject typos");
  }
}
