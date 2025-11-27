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

package vadl.iss.ppc64;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import vadl.iss.CosimTestUtils;

public class Ppc64TestBuilder {

  private final String name;
  private final List<String> instructions = new ArrayList<>();
  private boolean mode64 = false;

  public Ppc64TestBuilder(String name) {
    this.name = name;
  }

  // TODO: expand to 64 bit
  public BigInteger fillCR() {
    return fillCR(anyImmS(16));
  }

  public BigInteger fillCR(BigInteger value) {
    fillReg("0", value);
    add("mtcrf 255, 0");
    return value;
  }

  // TODO: expand to 64 bit
  public BigInteger fillReg(String reg) {
    return fillReg(reg, anyImmS(16));
  }

  public BigInteger fillReg(String reg, BigInteger value) {
    add("li %s, %s", reg, value);
    return value;
  }

  // TODO: expand to 64 bit
  public BigInteger fillMem(BigInteger mem) {
    return fillMem(mem, anyImmS(16));
  }

  public BigInteger fillMem(BigInteger mem, BigInteger value) {
    fillReg("0", mem);
    fillReg("1", value);
    add("stw 1, 0(0)");
    return value;
  }

  public Arbitrary<String> anyReg() {
    return Arbitraries.of(IntStream.range(0, 32).mapToObj(Integer::toString).toList());
  }

  public Arbitrary<String> anyRegExceptZero() {
    return Arbitraries.of(IntStream.range(1, 32).mapToObj(Integer::toString).toList());
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

  @FormatMethod
  public Ppc64TestBuilder add(String instr, Object... args) {
    instructions.add(String.format(instr, args));
    return this;
  }

  public Ppc64TestBuilder setMode64(boolean m) {
    this.mode64 = m;
    return this;
  }

  public String toAsmString() {
    return (mode64 ? "trap\n" : "") + String.join("\n", instructions);
  }

  public CosimTestUtils.TestCase toTestCase(int id) {
    String mode = mode64 ? "M64" : "M32";
    return new CosimTestUtils.TestCase(name + "-" + mode + "-" + id, toAsmString());
  }

}
