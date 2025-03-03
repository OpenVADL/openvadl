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

public class FormatFieldTest {
  @Test
  void slicedFormat() {
    var prog = """
        format I_TYPE : Bits<32> =
        { funct6 [31..26]
        , shamt  [25..20]
        , rs1    [19..15]
        , funct3 [14..12]
        , rd     [11..7]
        , opcode [6..0]
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void typedFormat() {
    var prog = """
        format I_TYPE : Bits<32> =
        { funct6 : Bits<6>
        , shamt  : Bits<6>
        , rs1    : Bits<5>
        , funct3 : Bits<3>
        , rd     : Bits<5>
        , opcode : Bits<7>
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void formatReferencesAreAllowed() {
    var prog = """
        format BYTE : Bits<8> = { bits [7..0] }
        format WORD : Bits<16> = { high: BYTE, low: BYTE }
        format ADDRESS : Bits<64> =
        { a: WORD
        , b: WORD
        , c: WORD
        , d: WORD
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }
}
