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

package vadl.iss;

import static vadl.TestUtils.arbitraryBetween;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;

public abstract class CosimTestBuilder {

  private final String testId;
  private final List<String> instructions = new ArrayList<>();

  public CosimTestBuilder(String testId) {
    this.testId = testId;
  }

  public abstract BigInteger fillReg(String reg, BigInteger value);
  public abstract BigInteger fillMem(BigInteger mem, BigInteger value);

  public BigInteger fillReg(String reg, BigInteger min, BigInteger max) {
    var val = arbitraryBetween(min, max)
        .sample();
    return fillReg(reg, val);
  }

  public BigInteger fillRegSigned(String reg, int size) {
    return fillReg(reg,
        BigInteger.valueOf(-2).pow(size - 1),
        BigInteger.valueOf(2)
            .pow(size - 1)
            .subtract(BigInteger.ONE)
    );
  }

  public BigInteger fillRegUnsigned(String reg, int size) {
    return fillReg(reg,
        BigInteger.ZERO,
        BigInteger.valueOf(2).pow(size).subtract(BigInteger.ONE)
    );
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
  public CosimTestBuilder add(String instr, Object... args) {
    instructions.add(String.format(instr, args));
    return this;
  }

  public String toAsmString() {
    return String.join("\n", instructions);
  }

  public CosimTestUtils.TestCase toTestCase() {
    return new CosimTestUtils.TestCase(testId, toAsmString());
  }

}
