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

package vadl.utils;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.DataType;
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
import vadl.viam.Constant;

public class DisassemblerTest extends AbstractTest {

  @Test
  void testDisassembly() {
    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");
    var isa = spec.isa().get();

    var disassembler = new Disassembler(isa, new RegularDecodeTreeGenerator(), BIG_ENDIAN);

    // bne x0, x0, 1234
    // 01001100000000000001100101100011
    var encoding = Constant.Value.fromInteger(
        new BigInteger("01001100000000000001100101100011", 2),
        DataType.bits(32));

    var result = disassembler.disassemble(encoding);
    assertEquals("BNE X0,X0,1234", result);
  }

}
