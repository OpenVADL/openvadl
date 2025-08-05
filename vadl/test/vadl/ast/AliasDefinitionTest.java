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

public class AliasDefinitionTest {

  private String prog(String content) {
    return """
        instruction set architecture TEST = {
        
          register A: Bits<64>
          register B: Bits<5> -> Bits<64>
          register C: Bits<32><64>
        
          %s
        }
        """.formatted(content);
  }

  @Test
  void constantRegister() {
    var prog = prog("""
        alias register X(i) = B(i)(12..0)
        """);
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

  }
}
