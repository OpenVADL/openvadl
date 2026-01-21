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

package vadl.iss.ppc64;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import vadl.iss.CosimTestUtils;

public class Ppc64TestBuilder {

  private static final int[] implementedSPRs = {
      0b00000_00001, // XER
      0b00000_01000, // LR
      0b00000_01001, // CTR
      0b11001_01111  // TAR
  };

  private static final int commentCol = 40;

  private final String name;
  private final int id;
  private final List<String> instructions = new ArrayList<>();

  public Ppc64TestBuilder(String name, int id) {
    this.name = name;
    this.id = id;
  }

  // loads a random 32-bit value into CR
  public BigInteger fillCR() {
    BigInteger value = fillReg("0");
    add("mtcrf 255, 0");
    return BigInteger.valueOf(value.intValue());
  }

  public BigInteger fillReg(String reg) {
    return fillReg(reg, anyImmS(32));
  }

  // loads a 32-bit signed value, which is then sign extended to 64 bits
  public BigInteger fillReg(String reg, BigInteger value) {
    int lo16 = value.and(BigInteger.valueOf(0xFFFF)).intValue();
    int hi16 = value.shiftRight(16).and(BigInteger.valueOf(0xFFFF)).intValue();
    String lis = String.format("lis %s, %s", reg, (short) hi16);
    String ori = String.format("ori %s, %s, %s", reg, reg, lo16);
    String preg = reg.length() == 1 ? " " + reg : reg;
    String lisLine =
        lis + " ".repeat(Math.max(0, commentCol - lis.length())) + " # X(" + preg + ") := "
            + toLoadedHexString(value);
    String oriLine = ori + " ".repeat(Math.max(0, commentCol - ori.length())) + " # ↑";
    instructions.add(lisLine);
    instructions.add(oriLine);
    int loadedVal = (hi16 << 16) | lo16;
    return BigInteger.valueOf(loadedVal);
  }

  // loads a random 32-bit value into memory
  public BigInteger fillMem(BigInteger mem) {
    fillReg("1", mem);
    BigInteger value = fillReg("2");
    add("stw 2, 0(1)");
    return value;
  }

  public Arbitrary<String> anyReg() {
    return Arbitraries.of(IntStream.range(0, 32).mapToObj(Integer::toString).toList());
  }

  public Arbitrary<String> anyRegExceptZero() {
    return Arbitraries.of(IntStream.range(1, 32).mapToObj(Integer::toString).toList());
  }

  public Arbitrary<String> anySpecialReg() {
    return Arbitraries.of(Arrays.stream(implementedSPRs)
        .mapToObj(String::valueOf).toArray(String[]::new));
  }

  public Arbitrary<String> anyCRField() {
    return Arbitraries.integers().between(0, 7).map(String::valueOf);
  }

  public Arbitrary<String> anyCRBit() {
    return Arbitraries.integers().between(0, 31).map(String::valueOf);
  }

  public BigInteger anyImmS(int bits) {
    var b = BigInteger.ONE.shiftLeft(bits - 1);
    return Arbitraries.bigIntegers()
        .greaterOrEqual(b.negate())
        .lessOrEqual(b.subtract(BigInteger.ONE))
        .sample();
  }

  public BigInteger anyImmU(int bits) {
    return Arbitraries.bigIntegers()
        .greaterOrEqual(BigInteger.ZERO)
        .lessOrEqual(BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE))
        .sample();
  }

  public BigInteger anyImmUFrom(int bits, BigInteger min) {
    return Arbitraries.bigIntegers()
        .greaterOrEqual(min)
        .lessOrEqual(BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE))
        .sample();
  }

  public BigInteger anySelectImmU(int bits) {
    int bitPosition = Arbitraries.integers().between(0, bits - 1).sample();
    return BigInteger.ONE.shiftLeft(bitPosition);
  }

  @FormatMethod
  public Ppc64TestBuilder add(String instr, Object... args) {
    instructions.add(String.format(instr, args));
    return this;
  }

  public String toAsmString() {
    return String.join("\n", instructions);
  }

  public CosimTestUtils.TestCase toTestCase() {
    return new CosimTestUtils.TestCase(name + "-" + id, false, toAsmString());
  }

  private static String toLoadedHexString(BigInteger value) {
    long signed64 = value.intValue();
    return "0x" + String.format("%016X", signed64) + " (" + signed64 + ")";
  }

}
