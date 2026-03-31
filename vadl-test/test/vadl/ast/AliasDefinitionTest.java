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

import org.junit.jupiter.api.Test;

public class AliasDefinitionTest {

  private String prog(String content) {
    return """
        instruction set architecture TEST = {
        
          register A: Bits<64>
          register B: Bits<5> -> Bits<64>
          register C: Bits<32><64>
          register D: Bits<32><32><64>
        
          %s
        
          format F : Bits<32> =
            { one : Bits<15>
            , two : Bits<15>
            , three : Bits<2>
            }
        }
        """.formatted(content);
  }

  @Test
  void constantRegister() {
    var prog = prog("""
        alias register W = A(12..0)
        alias register X = B(*)(12..0)
        alias register Y = C(*)(12..0)
        alias register U = D(*)(*)(12..0)
        
        function func(a: Bits<13>) -> Bits<13> = a
        
        instruction Test: F = {
          W := func(W)
          X(1) := func(X(2))
          Y(2) := func(Y(2))
          // U is not yet supported to write (even for normal registers)
        }
        encoding Test = { one = 0 }
        assembly Test = ""
        """);
    var spec = Frontend.compileToViam(prog);
    spec.verify();
  }
}
