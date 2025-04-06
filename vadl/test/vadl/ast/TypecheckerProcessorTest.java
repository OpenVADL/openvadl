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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.TestUtils;
import vadl.error.DiagnosticList;

public class TypecheckerProcessorTest {

  private static String base = """
      instruction set architecture ISA = {}
      micro processor MiP implements ISA = {
        %s
      }
      """;

  @Test
  void shouldThrow_duplicatedDefinitions() {
    var body = """
        start = 0x8000000
        start = 0x8000000
        firmware = {}
        firmware = {}
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = assertThrows(DiagnosticList.class, () -> typechecker.verify(ast));
    TestUtils.assertErrors(throwable,
        "Contains multiple `firmware` definitions",
        "Contains multiple `start` definitions"
    );
  }


  @Test
  void shouldThrow_missingStart() {
    var body = """
        """;
    var spec = base.formatted(body);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(spec), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = assertThrows(DiagnosticList.class, () -> typechecker.verify(ast));
    TestUtils.assertErrors(throwable,
        "Missing `start` address function."
    );
  }

}
