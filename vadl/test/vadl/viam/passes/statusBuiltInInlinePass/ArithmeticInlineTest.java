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


  // 32-bit tests Created with: https://godbolt.org/z/E4MYY5W15
  @TestFactory
  public Stream<DynamicTest> addsTests() {
    return runTests(
        adds(3, 0b111, 0b111, 0b110, true, false, true, false),
        adds(4, 0b1111, 0b0001, 0b0000, false, true, true, false),
        adds(4, 0b0100, 0b0100, 0b1000, true, false, false, true),
        adds(4, 0b1000, 0b1000, 0b0000, false, true, true, true),
        adds(32, 0x00L, 0x00L, 0x00L, false, true, false, false),
        adds(32, 0x00L, 0x80000000L, 0x80000000L, true, false, false, false),
        adds(32, 0x80000000L, 0x00L, 0x80000000L, true, false, false, false),
        adds(32, 0x00L, 0x7FFFFFFFL, 0x7FFFFFFFL, false, false, false, false),
        adds(32, 0x7FFFFFFFL, 0x00L, 0x7FFFFFFFL, false, false, false, false),
        adds(32, 0xFFFFFFFFL, 0x00L, 0xFFFFFFFFL, true, false, false, false),
        adds(32, 0x00L, 0xFFFFFFFFL, 0xFFFFFFFFL, true, false, false, false),
        adds(32, 0xFFFFFFFFL, 0x01L, 0x00L, false, true, true, false),
        adds(32, 0x01L, 0xFFFFFFFFL, 0x00L, false, true, true, false),
        adds(32, 0x10000000L, 0x10000000L, 0x20000000L, false, false, false, false),
        adds(32, 0x8FFFFFFFL, 0x8FFFFFFFL, 0x1FFFFFFEL, false, false, true, true),
        adds(32, 0x7FFFFFFFL, 0x8FFFFFFFL, 0xFFFFFFEL, false, false, true, false),
        adds(32, 0x8FFFFFFFL, 0x7FFFFFFFL, 0xFFFFFFEL, false, false, true, false)
    );
  }

  // Created with: https://godbolt.org/z/6aeY8PdfW
  @TestFactory
  public Stream<DynamicTest> addcTests() {
    return runTests(
        addc(32, 0x00L, 0x00L, false, 0x00L, false, true, false, false),
        addc(32, 0x00L, 0x00L, true, 0x01L, false, false, false, false),
        addc(32, 0x00L, 0x80000000L, true, 0x80000001L, true, false, false, false),
        addc(32, 0x00L, 0x80000000L, false, 0x80000000L, true, false, false, false),
        addc(32, 0x80000000L, 0x00L, true, 0x80000001L, true, false, false, false),
        addc(32, 0x80000000L, 0x00L, false, 0x80000000L, true, false, false, false),
        addc(32, 0x00L, 0x7FFFFFFFL, true, 0x80000000L, true, false, false, true),
        addc(32, 0x00L, 0x7FFFFFFFL, false, 0x7FFFFFFFL, false, false, false, false),
        addc(32, 0x7FFFFFFFL, 0x00L, true, 0x80000000L, true, false, false, true),
        addc(32, 0x7FFFFFFFL, 0x00L, false, 0x7FFFFFFFL, false, false, false, false),
        addc(32, 0xFFFFFFFFL, 0x00L, false, 0xFFFFFFFFL, true, false, false, false),
        addc(32, 0xFFFFFFFFL, 0x00L, true, 0x00L, false, true, true, false),
        addc(32, 0x00L, 0xFFFFFFFFL, false, 0xFFFFFFFFL, true, false, false, false),
        addc(32, 0x00L, 0xFFFFFFFFL, true, 0x00L, false, true, true, false),
        addc(32, 0xFFFFFFFFL, 0x01L, false, 0x00L, false, true, true, false),
        addc(32, 0xFFFFFFFFL, 0x01L, true, 0x01L, false, false, true, false),
        addc(32, 0x01L, 0xFFFFFFFFL, false, 0x00L, false, true, true, false),
        addc(32, 0x01L, 0xFFFFFFFFL, true, 0x01L, false, false, true, false),
        addc(32, 0x10000000L, 0x10000000L, false, 0x20000000L, false, false, false, false),
        addc(32, 0x10000000L, 0x10000000L, true, 0x20000001L, false, false, false, false),
        addc(32, 0x8FFFFFFFL, 0x8FFFFFFFL, false, 0x1FFFFFFEL, false, false, true, true),
        addc(32, 0x8FFFFFFFL, 0x8FFFFFFFL, true, 0x1FFFFFFFL, false, false, true, true),
        addc(32, 0x7FFFFFFFL, 0x8FFFFFFFL, false, 0xFFFFFFEL, false, false, true, false),
        addc(32, 0x7FFFFFFFL, 0x8FFFFFFFL, true, 0xFFFFFFFL, false, false, true, false),
        addc(32, 0x8FFFFFFFL, 0x7FFFFFFFL, false, 0xFFFFFFEL, false, false, true, false),
        addc(32, 0x8FFFFFFFL, 0x7FFFFFFFL, true, 0xFFFFFFFL, false, false, true, false)
    );
  }

  // 32 bit created with https://godbolt.org/z/3fW4GnGo9 on arm64
  @TestFactory
  public Stream<DynamicTest> subscTests() {
    return runTests(
        subsc(2, 0b00, 0b00, 0, false, true, true, false),
        subsc(2, 0b00, 0b01, -1, true, false, false, false),
        subsc(2, 0b00, 0b10, -2, true, false, false, true),
        subsc(2, 0b00, 0b11, 1, false, false, false, false),
        subsc(2, 0b01, 0b00, 1, false, false, true, false),
        subsc(2, 0b01, 0b01, 0, false, true, true, false),
        subsc(2, 0b01, 0b10, -1, true, false, false, true),
        subsc(2, 0b01, 0b11, -2, true, false, false, true),
        subsc(2, 0b10, 0b00, -2, true, false, true, false),
        subsc(2, 0b10, 0b01, 1, false, false, true, true),
        subsc(2, 0b10, 0b10, 0, false, true, true, false),
        subsc(2, 0b10, 0b11, -1, true, false, false, false),
        subsc(2, 0b11, 0b00, -1, true, false, true, false),
        subsc(2, 0b11, 0b01, -2, true, false, true, false),
        subsc(2, 0b11, 0b10, 1, false, false, true, false),
        subsc(2, 0b11, 0b11, 0, false, true, true, false),
        subsc(32, 0x00L, 0x00L, 0x00L, false, true, true, false),
        subsc(32, 0x00L, 0x80000000L, 0x80000000L, true, false, false, true),
        subsc(32, 0x80000000L, 0x00L, 0x80000000L, true, false, true, false),
        subsc(32, 0xFFFFFFFFL, 0x01L, 0xFFFFFFFEL, true, false, true, false),
        subsc(32, 0x01L, 0xFFFFFFFFL, 0x02L, false, false, false, false),
        subsc(32, 0x10000000L, 0x10000000L, 0x00L, false, true, true, false),
        subsc(32, 0x8FFFFFFFL, 0x8FFFFFFFL, 0x00L, false, true, true, false),
        subsc(32, 0x7FFFFFFFL, 0x8FFFFFFFL, 0xF0000000L, true, false, false, true),
        subsc(32, 0x8FFFFFFFL, 0x7FFFFFFFL, 0x10000000L, false, false, true, true),
        subsc(32, 0xFFFFFFFFL, 0x00L, 0xFFFFFFFFL, true, false, true, false),
        subsc(32, 0x00L, 0xFFFFFFFFL, 0x01L, false, false, false, false),
        subsc(32, 0x7FFFFFFFL, 0x00L, 0x7FFFFFFFL, false, false, true, false),
        subsc(32, 0x00L, 0x7FFFFFFFL, 0x80000001L, true, false, false, false)
    );
  }

  // 32 bit tests created with https://godbolt.org/z/qjEEYj8ro on x64
  @TestFactory
  public Stream<DynamicTest> subsbTests() {
    return runTests(
        subsb(2, 0b00, 0b00, 0, false, true, false, false),
        subsb(2, 0b00, 0b01, -1, true, false, true, false),
        subsb(2, 0b00, 0b10, -2, true, false, true, true),
        subsb(2, 0b00, 0b11, 1, false, false, true, false),
        subsb(2, 0b01, 0b00, 1, false, false, false, false),
        subsb(2, 0b01, 0b01, 0, false, true, false, false),
        subsb(2, 0b01, 0b10, -1, true, false, true, true),
        subsb(2, 0b01, 0b11, -2, true, false, true, true),
        subsb(2, 0b10, 0b00, -2, true, false, false, false),
        subsb(2, 0b10, 0b01, 1, false, false, false, true),
        subsb(2, 0b10, 0b10, 0, false, true, false, false),
        subsb(2, 0b10, 0b11, -1, true, false, true, false),
        subsb(2, 0b11, 0b00, -1, true, false, false, false),
        subsb(2, 0b11, 0b01, -2, true, false, false, false),
        subsb(2, 0b11, 0b10, 1, false, false, false, false),
        subsb(2, 0b11, 0b11, 0, false, true, false, false),
        subsb(32, 0x8FFFFFFFL, 0x8FFFFFFFL, 0x00L, false, true, false, false),
        subsb(32, 0x7FFFFFFFL, 0x8FFFFFFFL, 0xF0000000L, true, false, true, true),
        subsb(32, 0x8FFFFFFFL, 0x7FFFFFFFL, 0x10000000L, false, false, false, true),
        subsb(32, 0xFFFFFFFFL, 0x00L, 0xFFFFFFFFL, true, false, false, false),
        subsb(32, 0x00L, 0xFFFFFFFFL, 0x01L, false, false, true, false),
        subsb(32, 0x7FFFFFFFL, 0x00L, 0x7FFFFFFFL, false, false, false, false),
        subsb(32, 0x00L, 0x7FFFFFFFL, 0x80000001L, true, false, true, false)
    );
  }

  // 32 bit created with https://godbolt.org/z/3fW4GnGo9 on arm64
  @TestFactory
  public Stream<DynamicTest> subcTests() {
    return runTests(
        subc(2, 0b00, 0b00, false, -1, true, false, false, false),
        subc(2, 0b00, 0b00, true, 0, false, true, true, false),
        subc(2, 0b00, 0b01, false, -2, true, false, false, false),
        subc(2, 0b00, 0b01, true, -1, true, false, false, false),
        subc(2, 0b00, 0b10, false, 1, false, false, false, false),
        subc(2, 0b00, 0b10, true, -2, true, false, false, true),
        subc(2, 0b00, 0b11, false, 0, false, true, false, false),
        subc(2, 0b00, 0b11, true, 1, false, false, false, false),
        subc(2, 0b01, 0b00, false, 0, false, true, true, false),
        subc(2, 0b01, 0b00, true, 1, false, false, true, false),
        subc(2, 0b01, 0b01, false, -1, true, false, false, false),
        subc(2, 0b01, 0b01, true, 0, false, true, true, false),
        subc(2, 0b01, 0b10, false, -2, true, false, false, true),
        subc(2, 0b01, 0b10, true, -1, true, false, false, true),
        subc(2, 0b01, 0b11, false, 1, false, false, false, false),
        subc(2, 0b01, 0b11, true, -2, true, false, false, true),
        subc(2, 0b10, 0b00, false, 1, false, false, true, true),
        subc(2, 0b10, 0b00, true, -2, true, false, true, false),
        subc(2, 0b10, 0b01, false, 0, false, true, true, true),
        subc(2, 0b10, 0b01, true, 1, false, false, true, true),
        subc(2, 0b10, 0b10, false, -1, true, false, false, false),
        subc(2, 0b10, 0b10, true, 0, false, true, true, false),
        subc(2, 0b10, 0b11, false, -2, true, false, false, false),
        subc(2, 0b10, 0b11, true, -1, true, false, false, false),
        subc(2, 0b11, 0b00, false, -2, true, false, true, false),
        subc(2, 0b11, 0b00, true, -1, true, false, true, false),
        subc(2, 0b11, 0b01, false, 1, false, false, true, true),
        subc(2, 0b11, 0b01, true, -2, true, false, true, false),
        subc(2, 0b11, 0b10, false, 0, false, true, true, false),
        subc(2, 0b11, 0b10, true, 1, false, false, true, false),
        subc(2, 0b11, 0b11, false, -1, true, false, false, false),
        subc(2, 0b11, 0b11, true, 0, false, true, true, false),
        subc(32, 0x00L, 0x00L, false, 0xFFFFFFFFL, true, false, false, false),
        subc(32, 0x00L, 0x00L, true, 0x00L, false, true, true, false),
        subc(32, 0x00L, 0x80000000L, true, 0x80000000L, true, false, false, true),
        subc(32, 0x00L, 0x80000000L, false, 0x7FFFFFFFL, false, false, false, false),
        subc(32, 0x80000000L, 0x00L, true, 0x80000000L, true, false, true, false),
        subc(32, 0x80000000L, 0x00L, false, 0x7FFFFFFFL, false, false, true, true),
        subc(32, 0x00L, 0x7FFFFFFFL, true, 0x80000001L, true, false, false, false),
        subc(32, 0x00L, 0x7FFFFFFFL, false, 0x80000000L, true, false, false, false),
        subc(32, 0x7FFFFFFFL, 0x00L, true, 0x7FFFFFFFL, false, false, true, false),
        subc(32, 0x7FFFFFFFL, 0x00L, false, 0x7FFFFFFEL, false, false, true, false),
        subc(32, 0xFFFFFFFFL, 0x00L, false, 0xFFFFFFFEL, true, false, true, false),
        subc(32, 0xFFFFFFFFL, 0x00L, true, 0xFFFFFFFFL, true, false, true, false),
        subc(32, 0x00L, 0xFFFFFFFFL, false, 0x00L, false, true, false, false),
        subc(32, 0x00L, 0xFFFFFFFFL, true, 0x01L, false, false, false, false),
        subc(32, 0xFFFFFFFFL, 0x01L, false, 0xFFFFFFFDL, true, false, true, false),
        subc(32, 0xFFFFFFFFL, 0x01L, true, 0xFFFFFFFEL, true, false, true, false),
        subc(32, 0x01L, 0xFFFFFFFFL, false, 0x01L, false, false, false, false),
        subc(32, 0x01L, 0xFFFFFFFFL, true, 0x02L, false, false, false, false),
        subc(32, 0x10000000L, 0x10000000L, false, 0xFFFFFFFFL, true, false, false, false),
        subc(32, 0x10000000L, 0x10000000L, true, 0x00L, false, true, true, false),
        subc(32, 0x8FFFFFFFL, 0x8FFFFFFFL, false, 0xFFFFFFFFL, true, false, false, false),
        subc(32, 0x8FFFFFFFL, 0x8FFFFFFFL, true, 0x00L, false, true, true, false),
        subc(32, 0x7FFFFFFFL, 0x8FFFFFFFL, false, 0xEFFFFFFFL, true, false, false, true),
        subc(32, 0x7FFFFFFFL, 0x8FFFFFFFL, true, 0xF0000000L, true, false, false, true),
        subc(32, 0x8FFFFFFFL, 0x7FFFFFFFL, false, 0xFFFFFFFL, false, false, true, true),
        subc(32, 0x8FFFFFFFL, 0x7FFFFFFFL, true, 0x10000000L, false, false, true, true)
    );
  }

  // 32 bit tests created with https://godbolt.org/z/qjEEYj8ro on x64
  @TestFactory
  public Stream<DynamicTest> subbTests() {
    return runTests(
        subb(2, 0b00, 0b00, false, 0, false, true, false, false),
        subb(2, 0b00, 0b00, true, -1, true, false, true, false),
        subb(2, 0b00, 0b01, false, -1, true, false, true, false),
        subb(2, 0b00, 0b01, true, -2, true, false, true, false),
        subb(2, 0b00, 0b10, false, -2, true, false, true, true),
        subb(2, 0b00, 0b10, true, 1, false, false, true, false),
        subb(2, 0b00, 0b11, false, 1, false, false, true, false),
        subb(2, 0b00, 0b11, true, 0, false, true, true, false),
        subb(2, 0b01, 0b00, false, 1, false, false, false, false),
        subb(2, 0b01, 0b00, true, 0, false, true, false, false),
        subb(2, 0b01, 0b01, false, 0, false, true, false, false),
        subb(2, 0b01, 0b01, true, -1, true, false, true, false),
        subb(2, 0b01, 0b10, false, -1, true, false, true, true),
        subb(2, 0b01, 0b10, true, -2, true, false, true, true),
        subb(2, 0b01, 0b11, false, -2, true, false, true, true),
        subb(2, 0b01, 0b11, true, 1, false, false, true, false),
        subb(2, 0b10, 0b00, false, -2, true, false, false, false),
        subb(2, 0b10, 0b00, true, 1, false, false, false, true),
        subb(2, 0b10, 0b01, false, 1, false, false, false, true),
        subb(2, 0b10, 0b01, true, 0, false, true, false, true),
        subb(2, 0b10, 0b10, false, 0, false, true, false, false),
        subb(2, 0b10, 0b10, true, -1, true, false, true, false),
        subb(2, 0b10, 0b11, false, -1, true, false, true, false),
        subb(2, 0b10, 0b11, true, -2, true, false, true, false),
        subb(2, 0b11, 0b00, false, -1, true, false, false, false),
        subb(2, 0b11, 0b00, true, -2, true, false, false, false),
        subb(2, 0b11, 0b01, false, -2, true, false, false, false),
        subb(2, 0b11, 0b01, true, 1, false, false, false, true),
        subb(2, 0b11, 0b10, false, 1, false, false, false, false),
        subb(2, 0b11, 0b10, true, 0, false, true, false, false),
        subb(2, 0b11, 0b11, false, 0, false, true, false, false),
        subb(2, 0b11, 0b11, true, -1, true, false, true, false),
        subb(32, 0x00L, 0x00L, false, 0x00L, false, true, false, false),
        subb(32, 0x00L, 0x00L, true, 0xFFFFFFFFL, true, false, true, false),
        subb(32, 0x00L, 0x80000000L, true, 0x7FFFFFFFL, false, false, true, false),
        subb(32, 0x00L, 0x80000000L, false, 0x80000000L, true, false, true, true),
        subb(32, 0x80000000L, 0x00L, true, 0x7FFFFFFFL, false, false, false, true),
        subb(32, 0x80000000L, 0x00L, false, 0x80000000L, true, false, false, false),
        subb(32, 0x00L, 0x7FFFFFFFL, true, 0x80000000L, true, false, true, false),
        subb(32, 0x00L, 0x7FFFFFFFL, false, 0x80000001L, true, false, true, false),
        subb(32, 0x7FFFFFFFL, 0x00L, true, 0x7FFFFFFEL, false, false, false, false),
        subb(32, 0x7FFFFFFFL, 0x00L, false, 0x7FFFFFFFL, false, false, false, false),
        subb(32, 0xFFFFFFFFL, 0x00L, false, 0xFFFFFFFFL, true, false, false, false),
        subb(32, 0xFFFFFFFFL, 0x00L, true, 0xFFFFFFFEL, true, false, false, false),
        subb(32, 0x00L, 0xFFFFFFFFL, false, 0x01L, false, false, true, false),
        subb(32, 0x00L, 0xFFFFFFFFL, true, 0x00L, false, true, true, false),
        subb(32, 0xFFFFFFFFL, 0x01L, false, 0xFFFFFFFEL, true, false, false, false),
        subb(32, 0xFFFFFFFFL, 0x01L, true, 0xFFFFFFFDL, true, false, false, false),
        subb(32, 0x01L, 0xFFFFFFFFL, false, 0x02L, false, false, true, false),
        subb(32, 0x01L, 0xFFFFFFFFL, true, 0x01L, false, false, true, false),
        subb(32, 0x10000000L, 0x10000000L, false, 0x00L, false, true, false, false),
        subb(32, 0x10000000L, 0x10000000L, true, 0xFFFFFFFFL, true, false, true, false),
        subb(32, 0x8FFFFFFFL, 0x8FFFFFFFL, false, 0x00L, false, true, false, false),
        subb(32, 0x8FFFFFFFL, 0x8FFFFFFFL, true, 0xFFFFFFFFL, true, false, true, false),
        subb(32, 0x7FFFFFFFL, 0x8FFFFFFFL, false, 0xF0000000L, true, false, true, true),
        subb(32, 0x7FFFFFFFL, 0x8FFFFFFFL, true, 0xEFFFFFFFL, true, false, true, true),
        subb(32, 0x8FFFFFFFL, 0x7FFFFFFFL, false, 0x10000000L, false, false, false, true),
        subb(32, 0x8FFFFFFFL, 0x7FFFFFFFL, true, 0xFFFFFFFL, false, false, false, true)
    );
  }

  private Stream<Test> adds(int size, long a, long b, long result, boolean negative, boolean zero,
                            boolean carry, boolean overflow) {
    return binary(BuiltInTable.ADDS, size, a, b, result, negative, zero, carry, overflow);
  }

  private Stream<Test> addc(int size, long a, long b, boolean c, long result, boolean negative,
                            boolean zero,
                            boolean carry, boolean overflow) {
    return ternary(BuiltInTable.ADDC, size, a, b, c, result, negative, zero, carry, overflow);
  }

  private Stream<Test> subsc(int size, long a, long b, long result, boolean negative, boolean zero,
                             boolean carry, boolean overflow) {
    return binary(BuiltInTable.SUBSC, size, a, b, result, negative, zero, carry, overflow);
  }

  private Stream<Test> subsb(int size, long a, long b, long result, boolean negative, boolean zero,
                             boolean carry, boolean overflow) {
    return binary(BuiltInTable.SUBSB, size, a, b, result, negative, zero, carry, overflow);
  }

  private Stream<Test> subc(int size, long a, long b, boolean c, long result, boolean negative,
                            boolean zero,
                            boolean carry, boolean overflow) {
    return ternary(BuiltInTable.SUBC, size, a, b, c, result, negative, zero, carry, overflow);
  }

  private Stream<Test> subb(int size, long a, long b, boolean c, long result, boolean negative,
                            boolean zero,
                            boolean carry, boolean overflow) {
    return ternary(BuiltInTable.SUBB, size, a, b, c, result, negative, zero, carry, overflow);
  }

  private Stream<Test> binary(BuiltInTable.BuiltIn op, int size, long a, long b, long result,
                              boolean negative, boolean zero,
                              boolean carry, boolean overflow) {
    return operation(op,
        List.of(
            Constant.Value.of(a, Type.bits(size)),
            Constant.Value.of(b, Type.bits(size))
        ),
        Constant.Value.of(result, Type.bits(size)),
        negative, zero, carry, overflow
    );
  }

  private Stream<Test> ternary(BuiltInTable.BuiltIn op, int size, long a, long b, boolean c,
                               long result,
                               boolean negative,
                               boolean zero,
                               boolean carry, boolean overflow) {
    return operation(op,
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
