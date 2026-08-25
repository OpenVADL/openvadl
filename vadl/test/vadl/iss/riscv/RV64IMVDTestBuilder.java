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

package vadl.iss.riscv;

import java.math.BigInteger;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;

public class RV64IMVDTestBuilder extends RV64IMVTestBuilder {

  public RV64IMVDTestBuilder(String testId) {
    super(testId);
  }

  public BigInteger fillFloatReg(String reg, BigInteger value, boolean single) {
    add("li t0, 0x%s", value.toString(16));
    add("fmv.%s.x %s, t0", single ? "s" : "d", reg);
    return value;
  }

  public Arbitrary<String> anyTempFloatReg() {
    return Arbitraries.of(
        "f0", "f1", "f2", "f3", "f4", "f5", "f6", "f7",
        "f28", "f29", "f30", "f31"
    );
  }

  /**
   * Configures the CPU for float operations by modifying the machine status (mstatus) register.
   *
   * <p>This method enables float operations by setting the FS field in the mstatus
   * register to `0b11` (i.e. "dirty") to avoid illegal instruction exceptions.
   *
   * @param tmpReg The name of a temporary general-purpose register
   *               used to hold intermediate values.
   */
  public void configureCpuForFloatOps(String tmpReg) {
    add("# configure cpu for float");
    add("li %s, 0x6000", tmpReg);
    add("csrs mstatus, %s", tmpReg);
  }

}
