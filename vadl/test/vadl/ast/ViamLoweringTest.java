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
import vadl.error.Diagnostic;

public class ViamLoweringTest {

  private final String base = """
       instruction set architecture ISA = {
        register file X : Bits<5> -> Bits<32>
            
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
          pseudo return instruction = DOESNOTEXIST
          pseudo call instruction = NOP
          pseudo local address load instruction = NOP
          pseudo absolute address load instruction = NOP
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
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    var throwable =
        Assertions.assertThrows(Diagnostic.class, () -> new ViamLowering().generate(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Cannot find the pseudo return instruction", throwable.reason);
  }

  @Test
  void shouldThrow_whenPseudoCallInstructionMissing() {
    var prog = """
          pseudo return instruction = NOP
          pseudo call instruction = DOESNOTEXIST
          pseudo local address load instruction = NOP
          pseudo absolute address load instruction = NOP
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
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    var throwable =
        Assertions.assertThrows(Diagnostic.class, () -> new ViamLowering().generate(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Cannot find the pseudo call instruction", throwable.reason);
  }}
