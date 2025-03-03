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

import static vadl.ast.AstTestUtils.assertAstEquality;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Test;

public class InstructionTest {

  @Test
  void parseCombinedInstructionDefinition() {
    var prog = """
        instruction set architecture RV32I = {
          register file X : Bits<5> -> Bits<32>
          format R_TYPE : Bits<32> = {
            funct7 [31..25],
            rs2    [24..20],
            rs1    [19..15],
            funct3 [14..12],
            rd     [11..7],
            opcode [6..0]
          }

          instruction ADD : R_TYPE = {
            X(rd) := X(rs1) + X(rs2)
          }

          encoding ADD = {
            opcode = 0b011'0011,
            funct3 = 0b000,
            funct7 = 0b000'0000
          }

          assembly ADD = (mnemonic, " ", rd, ", ", rs1, ", ", rs2)
        }
        """;

    var ast = VadlParser.parse(prog);
    verifyPrettifiedAst(ast);
  }

  @Test
  void noneIsValidEncodingKeyword() {
    var prog1 = """
        instruction set architecture TEST = {
          model Encoding (head: Encs) : Encs = {
            $head
          , d = 4
          , none
          }
        
          format R_TYPE : Bits<32> = {
            a: Bits<10>, b: Bits<10>, c: Bits<10>, d: Bits<10>
          }

          instruction ADD : R_TYPE = {}

          encoding ADD = {
            a = 1,
            $Encoding(b = 2, none, c=3)
          }
        }
        """;

    var prog2 = """
        instruction set architecture TEST = {
          format R_TYPE : Bits<32> = {
            a: Bits<10>, b: Bits<10>, c: Bits<10>, d: Bits<10>
          }
          instruction ADD : R_TYPE = {}
          encoding ADD = {
            a = 1
          , b = 2
          , c = 3
          , d = 4
          }
        }
        """;

    Ast parse = VadlParser.parse(prog1);
    assertAstEquality(parse, VadlParser.parse(prog2));
  }
}
