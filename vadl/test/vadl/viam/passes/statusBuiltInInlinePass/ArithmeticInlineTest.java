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

package vadl.viam.passes.statusBuiltInInlinePass;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;

public class ArithmeticInlineTest extends StatusBuiltinInlineTest {


  @TestFactory
  public Stream<DynamicTest> addsTests() {
    return runTests(
        adds(5, 0b0, 0b0, 0b0, false, true, false, false),
        adds(3, 0b111, 0b111, 0b110, true, false, true, false),
        adds(4, 0b1111, 0b0001, 0b0000, false, true, true, false),
        adds(4, 0b0100, 0b0100, 0b1000, true, false, false, true),
        adds(4, 0b1000, 0b1000, 0b0000, false, true, true, true)
    );
  }

  @TestFactory
  public Stream<DynamicTest> addcTests() {
    return runTests(
        addc(4, 0b0000, 0b0000, false, 0b0000, false, true, false, false),
        addc(4, 0b0000, 0b0000, true, 0b0001, false, false, false, false),
        addc(32, 0xFFFFFFFFL, 0x01L, false, 0x00L, false, true, true, false),
        addc(32, 0xFFFFFFFEL, 0x01L, false, 0xFFFFFFFFL, true, false, false, false),
        addc(32, 0xFFFFFFFFL, 0x00L, false, 0xFFFFFFFFL, true, false, false, false),
        addc(32, 0xFFFFFFFFL, 0x00L, true, 0x00L, false, true, true, false)
    );
  }


  private Stream<Test> adds(int size, long a, long b, long result, boolean negative, boolean zero,
                            boolean carry, boolean overflow) {
    return operation(BuiltInTable.ADDS,
        List.of(
            Constant.Value.of(a, Type.bits(size)),
            Constant.Value.of(b, Type.bits(size))
        ),
        Constant.Value.of(result, Type.bits(size)),
        negative, zero, carry, overflow
    );
  }

  private Stream<Test> addc(int size, long a, long b, boolean c, long result, boolean negative,
                            boolean zero,
                            boolean carry, boolean overflow) {
    return operation(BuiltInTable.ADDC,
        List.of(
            Constant.Value.of(a, Type.bits(size)),
            Constant.Value.of(b, Type.bits(size)),
            Constant.Value.of(c)
        ),
        Constant.Value.of(result, Type.bits(size)),
        negative, zero, carry, overflow
    );
  }

}
