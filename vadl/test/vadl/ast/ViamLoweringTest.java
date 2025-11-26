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
import vadl.TestUtils;
import vadl.error.DiagnosticList;

public class ViamLoweringTest {

  private final String base = """
       instruction set architecture ISA = {
        register X : Bits<5> -> Bits<32>
      
        pseudo instruction NOP( symbol: Bits<5>) = {
        }
        assembly NOP = (mnemonic)
      }
      
      """;

  private String inputWrappedByValidAbi(String input) {
    return """
          %s
        
          application binary interface ABI for ISA = {
            %s
          }
        """.formatted(base, input);
  }

  @Test
  void shouldThrow_whenPseudoReturnInstructionMissing() {
    var prog = """
          special return instruction = DOESNOTEXIST
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var throwable = Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)));
    TestUtils.assertErrors(throwable, "Unknown Symbol: \"DOESNOTEXIST\"");
  }

  @Test
  void shouldThrow_whenPseudoCallInstructionMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = DOESNOTEXIST
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var throwable = Assertions.assertThrows(DiagnosticList.class,
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)));
    TestUtils.assertErrors(throwable, "Unknown Symbol: \"DOESNOTEXIST\"");
  }

  @Test
  public void dynamicTensorSlicingForAllDo() {
    var prog = """
          instruction set architecture Tensor = {
            using Index = Bits<4>
            register X : Bits<16><4><32>

            format F : Bits<16> = {opcode : Bits<4>, rs2: Index, rs1: Index, rd: Index}

            instruction ADD4V : F = forall i: Bits<4> in 0 .. 3 do X(rd)(i) := X(rs1)(i) + X(rs2)(i)
            encoding ADD4V = {opcode = 0b1100}
            assembly ADD4V = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
          }
        """;
    Assertions.assertDoesNotThrow(() -> TestUtils.compileToViam(prog), "Cannot lower VIAM");
  }

  @Test
  public void dynamicTensorSlicingForAllTensor() {
    var prog = """
          instruction set architecture Tensor = {
            using QInd = Bits<4>
            register Q : UInt<16><4><32>

            format F : Bits<16> = {opcode : Bits<4>, rs2: QInd, rs1: QInd, rd: QInd}

            instruction QADD : F = {
              let v = Q(rs1) in
              let m = Q(rs2) in
              let sum = forall i: Bits<4> in 0 .. 3 tensor v(i) + m(i) in
              let p = sum as UInt<4><32> in
              Q(rd) := p
            }

            encoding QADD = {opcode = 0b1100}
            assembly QADD = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
          }
        """;
    Assertions.assertDoesNotThrow(() -> TestUtils.compileToViam(prog), "Cannot lower VIAM");
  }
}
