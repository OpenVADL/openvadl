// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

public class ShiftRotateInlineTest extends StatusBuiltinInlineTest {

  @TestFactory
  public Stream<DynamicTest> lslsTests() {
    return runTests(
        // single bit
        lsls(0b0, 1, 0, 1, 0b0, false, true, false),
        lsls(0b1, 1, 0, 1, 0b1, true, false, false),
        lsls(0b0, 1, 1, 1, 0b0, false, true, false),
        lsls(0b1, 1, 1, 1, 0b0, false, true, true),

        // shifting zero bits
        lsls(0b0000, 4, 0, 1, 0b0000, false, true, false),
        lsls(0b1000, 4, 0, 1, 0b1000, true, false, false),

        // shifting one bit
        lsls(0b0000, 4, 1, 1, 0b0000, false, true, false),
        lsls(0b1000, 4, 1, 1, 0b0000, false, true, true),
        lsls(0b0111, 4, 1, 1, 0b1110, true, false, false),
        lsls(0b1111, 4, 1, 1, 0b1110, true, false, true),
        lsls(0b0000, 4, 1, 1, 0b0000, false, true, false),
        lsls(0b0001, 4, 1, 1, 0b0010, false, false, false),
        lsls(0b1110, 4, 1, 1, 0b1100, true, false, true),
        lsls(0b1111, 4, 1, 1, 0b1110, true, false, true),

        // shifting multiple bits
        lsls(0b01001011, 8, 2, 2, 0b00101100, false, false, true),
        lsls(0b11001011, 8, 3, 2, 0b01011000, false, false, false),
        lsls(0b01001011, 8, 4, 3, 0b10110000, true, false, false),
        lsls(0b11001011, 8, 5, 3, 0b01100000, false, false, true),

        // shift amount around bit width
        lsls(0xFFFFFFFFL, 32, 31, 6, 0x80000000L, true, false, true),
        lsls(0xFFFFFFFFL, 32, 32, 6, 0x00000000, false, true, true),
        lsls(0xFFFFFFFFL, 32, 33, 6, 0x00000000, false, true, true),
        lsls(0x0, 32, 31, 6, 0x0, false, true, false),
        lsls(0x0, 32, 32, 6, 0x0, false, true, false),
        lsls(0x0, 32, 33, 6, 0x0, false, true, false),

        // shift large amount
        lsls(0xFFFFFFFFL, 32, 0xFFFF, 32, 0x0, false, true, true),
        lsls(0x0,         32, 0xFFFF, 32, 0x0, false, true, false),

        // odd sizes
        lsls(0b0101110, 7, 1, 1, 0b1011100, true, false, false),
        lsls(0b0101110, 7, 3, 2, 0b1110000, true, false, false),
        lsls(0b0101111, 7, 6, 3, 0b1000000, true, false, true),
        lsls(0b0101111, 7, 7, 3, 0, false, true, true),
        lsls(0b0101110, 7, 0xFFFF, 32, 0, false, true, false),

        lsls(0b01011101011010110, 17, 1, 1, 0b10111010110101100, true, false, false),
        lsls(0b01011101011010110, 17, 3, 2, 0b11101011010110000, true, false, false),
        lsls(0b01011101011010111, 17, 16, 8, 0b10000000000000000, true, false, true),
        lsls(0b01011101011010110, 17, 17, 8, 0, false, true, false),
        lsls(0b01011101011010111, 17, 0xFFFF, 32, 0, false, true, true)
    );
  }

  @TestFactory
  public Stream<DynamicTest> rorsTests() {
    return runTests(
        // single bit
        rors(0b0, 1, 0, 1, 0b0, false, true, false),
        rors(0b1, 1, 0, 1, 0b1, true, false, false),
        rors(0b0, 1, 1, 1, 0b0, false, true, false),
        rors(0b1, 1, 1, 1, 0b1, true, false, true),

        // rotating zero bits
        rors(0b0000, 4, 0, 1, 0b0000, false, true, false),
        rors(0b1000, 4, 0, 1, 0b1000, true, false, false),

        // rotating one bit
        rors(0b0000, 4, 1, 1, 0b0000, false, true, false),
        rors(0b1000, 4, 1, 1, 0b0100, false, false, false),
        rors(0b0111, 4, 1, 1, 0b1011, true, false, true),
        rors(0b1111, 4, 1, 1, 0b1111, true, false, true),
        rors(0b0000, 4, 1, 1, 0b0000, false, true, false),
        rors(0b0001, 4, 1, 1, 0b1000, true, false, true),
        rors(0b1110, 4, 1, 1, 0b0111, false, false, false),
        rors(0b1111, 4, 1, 1, 0b1111, true, false, true),

        // rotating multiple bits
        rors(0b01001011, 8, 2, 2, 0b11010010, true, false, true),
        rors(0b11001011, 8, 3, 2, 0b01111001, false, false, false),
        rors(0b01001011, 8, 4, 3, 0b10110100, true, false, true),
        rors(0b11001011, 8, 5, 3, 0b01011110, false, false, false),

        // rotate amount around bit width
        rors(0xFFFFFFFFL, 32, 31, 6, 0xFFFFFFFFL, true, false, true),
        rors(0xFFFFFFFFL, 32, 32, 6, 0xFFFFFFFFL, true, false, true),
        rors(0xFFFFFFFFL, 32, 33, 6, 0xFFFFFFFFL, true, false, true),
        rors(0x0, 32, 31, 6, 0x0, false, true, false),
        rors(0x0, 32, 32, 6, 0x0, false, true, false),
        rors(0x0, 32, 33, 6, 0x0, false, true, false),

        // rotate large amount
        rors(0xFFFFFFFFL, 32, 0xFFFF, 32, 0xFFFFFFFFL, true, false, true),
        rors(0x0,         32, 0xFFFF, 32, 0x0,         false, true, false),

        // odd sizes
        rors(0b0101110, 7, 1, 1, 0b0010111, false, false, false),
        rors(0b0101110, 7, 3, 2, 0b1100101, true, false, true),
        rors(0b0101111, 7, 6, 3, 0b1011110, true, false, true),
        rors(0b0101111, 7, 7, 3, 0b0101111, false, false, false),
        rors(0b0101110, 7, 0xFFFF, 32, 0b0010111, false, false, false),

        rors(0b01011101011010110, 17, 1, 1, 0b00101110101101011, false, false, false),
        rors(0b01011101011010110, 17, 3, 2, 0b11001011101011010, true, false, true),
        rors(0b01011101011010111, 17, 16, 8, 0b10111010110101110, true, false, true),
        rors(0b01011101011010110, 17, 17, 8, 0b01011101011010110, false, false, false),
        rors(0b01011101011010111, 17, 0xFFFF, 32, 0b01011101011010111, false, false, false)
    );
  }

  private Stream<Test> lsls(long a, int sizeA, long b, int sizeB,
                            long result, boolean negative, boolean zero, boolean carry) {
    return binary(BuiltInTable.LSLS, a, sizeA, b, sizeB, result, negative, zero, carry);
  }

  private Stream<Test> rors(long a, int sizeA, long b, int sizeB,
                            long result, boolean negative, boolean zero, boolean carry) {
    return binary(BuiltInTable.RORS, a, sizeA, b, sizeB, result, negative, zero, carry);
  }

  private Stream<Test> binary(BuiltInTable.BuiltIn op, long a, int sizeA, long b, int sizeB,
                              long result, boolean negative, boolean zero, boolean carry) {
    return operation(op,
        List.of(
            Constant.Value.of(a, Type.bits(sizeA)),
            Constant.Value.of(b, Type.bits(sizeB))
        ),
        Constant.Value.of(result, Type.bits(sizeA)),
        negative, zero, carry, false
    );
  }
}
