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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.Diagnostic;

public class ExceptionTest {

  private static String base = """
      instruction set architecture ISA = {
        constant c = 0x2
        exception E1 = {
        }
      
        exception E2(t: Bits<c + 3>, b: Bits<10>) = {
        }
      
        function F1 -> Bits<5> = 0
      
        enumeration En: Bits<5> = {
          ONE = 0x2,
          TWO = 0x4
        }
      
        instruction Test : f = {
          %s
        }
        assembly Test = ""
        encoding Test = { f1 = 0 }
      
      
        format f: Bits<5> = {
          f1: Bits<5>
        }
      }
      processor MiP implements ISA = {
      }
      """;


  @Test
  void valid_exceptionNoArgs() {
    var body = """
        raise E1
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    var lowering = new ViamLowering();
    var viam = lowering.generate(ast);
    assertEquals("ISA", viam.isa().get().simpleName());
  }

  @Test
  void valid_exceptionArgs() {
    var body = """
        raise E2(En::ONE, 1)
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    var lowering = new ViamLowering();
    var viam = lowering.generate(ast);
    assertEquals("ISA", viam.isa().get().simpleName());
  }


  @Test
  void shouldThrow_wrongNumberOfArgs() {
    var body = """
        raise E1(1, 2)
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    org.assertj.core.api.Assertions.assertThat(throwable.getMessage())
        .contains("Expected 0 arguments but got `2`");
  }

  @Test
  void shouldThrow_wrongArgSize() {
    var body = """
        raise E2(200, 2)
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    org.assertj.core.api.Assertions.assertThat(throwable.getMessage())
        .contains("Expected `Bits<5>` but got `Bits<8>`.");
  }

  // TODO: Currently this check is implicit given the return type.
  //    However, we want that check explicitly.
  @Test
  void shouldThrow_notAnException() {
    var body = """
        raise F1
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    org.assertj.core.api.Assertions.assertThat(throwable.getMessage())
        .contains("Expected `void` but got `Bits<5>`.");
  }

}
