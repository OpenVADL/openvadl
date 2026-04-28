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

package vadl.iss.aarch64;

public class A64SVETestBuilder extends A64TestBuilder {

  public A64SVETestBuilder(String testId) {
    super(testId);
  }

  /**
   * Placeholder hook for SVE setup.
   * Only emits operations modeled by the current AArch64 spec.
   */
  public void configureCpuForSveOps(String tmpReg, int vlBits) {
    if (vlBits <= 0) {
      throw new IllegalArgumentException("SVE VL must be > 0, got " + vlBits);
    }
    if (vlBits % 128 != 0) {
      throw new IllegalArgumentException("SVE VL must be a multiple of 128, got " + vlBits);
    }
    var zcrLen = (vlBits / 128) - 1;
    add("# configure CPACR to enable SIMD/SVE access in reference QEMU");
    add("mov %s, #0x330000", tmpReg);
    add("msr cpacr_el1, %s", tmpReg);
    add("# match reference QEMU's runtime SVE vector length to the fixed VADL test VL");
    add("mov %s, #%d", tmpReg, zcrLen);
    add("msr zcr_el1, %s", tmpReg);
  }

  public void fillMemory64(long addr, int words, String addrReg, String dataReg) {
    add("ldr %s, =0x%x", addrReg, addr);
    for (int i = 0; i < words; i++) {
      fillRegUnsigned(dataReg, 64);
      add("str %s, [%s, #%d]", dataReg, addrReg, i * 8);
    }
  }

  public void loadZFromMemory(String zregister, long addr, String addrReg) {
    add("ldr %s, =0x%x", addrReg, addr);
    add("ldr %s, [%s]", zregister, addrReg);
  }

  public void storeZToMemory(String zregister, long addr, String addrReg) {
    add("ldr %s, =0x%x", addrReg, addr);
    add("str %s, [%s]", zregister, addrReg);
  }

  public void loadMemory64ToRegs(long addr, int words, int firstReg, String addrReg) {
    add("ldr %s, =0x%x", addrReg, addr);
    for (int i = 0; i < words; i++) {
      add("ldr x%d, [%s, #%d]", firstReg + i, addrReg, i * 8);
    }
  }

  public void setPredicateAllTrue(String predicateRegister, String elemSuffix) {
    add("ptrue %s.%s", predicateRegister, elemSuffix);
  }
}
