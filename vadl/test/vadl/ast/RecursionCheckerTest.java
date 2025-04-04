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

public class RecursionCheckerTest {
  @Test
  public void constantRecursionThrowsTest() {
    var prog = """
        constant a = b
        constant b = a 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void enumerationFieldRecursionThrowsTest() {
    var prog = """
        enumeration rec : SInt<32> =
          { a = rec::b
          , b = rec::a
          }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void formatRecursionThrowsTest() {
    var prog = """
        format rec : Bits<16> = { a : rec }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void usingRecursionThrowsTest() {
    var prog = """
        using a = b
        using b = a
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void aliasRecursionThrowsTest() {
    var prog = """
        instruction set architecture ISA = {
          alias register a = b
          alias register b = a
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void constantFunctionRecursionThrowsTest() {
    var prog = """
        constant a = b (1)
        function b (x : SInt<32>) -> SInt<32> = a
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void typeSystemRecursionThrowsTest() {
    var prog = """
        constant x: Bits<y> = 1
        constant y: Bits<x> = 1
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }

  @Test
  public void typeSystemConstantRecursionThrowsTest() {
    var prog = """
        constant a = b
        constant b: Bits<a> = 3
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diagnostic = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals("Infinite Recursion", diagnostic.reason);
  }
}

