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

package vadl.iss.riscv;

import java.math.BigInteger;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import vadl.iss.AsmTestBuilder;

public class RV64IMTestBuilder extends AsmTestBuilder {

  public RV64IMTestBuilder(String testId) {
    super(testId);
  }

  @Override
  public BigInteger fillReg(String reg, BigInteger value) {
    add("li %s, %s", reg, value);
    return value;
  }

  @Override
  public Arbitrary<String> anyTempReg() {
    return Arbitraries.of("x5", "x7", "x28", "x29", "x30", "x31");
  }

  @Override
  public Arbitrary<String> anyReg() {
    return Arbitraries.of(IntStream.range(0, 32).mapToObj(i -> "x" + i).toList());
  }

}
