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

package vadl.iss.riscv;

import static vadl.TestUtils.arbitraryUnsignedInt;

import java.math.BigInteger;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import vadl.iss.AsmTestBuilder;

public class RV64IMVTestBuilder extends AsmTestBuilder {

  public RV64IMVTestBuilder(String testId) {
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

  /**
   * Fills a vector register with data.
   * The method writes 32 random 32-bit unsigned integer values to consecutive
   * memory locations starting at the given address, loads them into the specified
   * vector register, and utilizes temporary registers for intermediate operations.
   *
   * @param vecReg  The name of the vector register to be filled with data.
   * @param addr    The starting memory address where the 32 elements will be written.
   * @param tmpReg1 The name of the temporary register used to hold the base address.
   * @param tmpReg2 The name of the temporary register used for intermediate data values.
   */
  public void fillVectorAddr(String vecReg, long addr, String tmpReg1,
                             String tmpReg2) {
    // load the destination addr into tmpReg1
    add("li %s, 0x%x", tmpReg1, addr);
    // write 32 32-bit elements to the respective address with random values
    for (int i = 0; i < 32; i++) {
      fillReg(tmpReg2, arbitraryUnsignedInt(64).sample());
      add("sw %s, %d(%s)", tmpReg2, i * 4, tmpReg1);
    }
    add("vle32.v  %s, (%s)", vecReg, tmpReg1);
  }

  /**
   * Configures the CPU for vector operations by modifying the machine status (mstatus) register
   * and setting the vector length and vector type.
   *
   * <p>This method enables vector operations by setting the VS field in the mstatus
   * register to `0b11` to avoid illegal instruction exceptions.
   * It also initializes the vector length (vl) and vector type (vtype) registers required
   * for vector processing.
   *
   * @param tmpReg The name of a temporary general-purpose register
   *               used to hold intermediate values.
   */
  public void configureCpuForVecOps(String tmpReg) {
    // the csrs mstatus register must set the VS field to 0b11, otherwise
    // it will result in an illegal instruction exception.
    // additionally, we must set the vl and vtype registers to configure the vector length and
    // element size.
    // while this isn't necessary for VADL's generated QEMU (because we only support fixed
    // vl and vtype operations), this has to be done for UPSTREAM.
    add("# configure cpu");
    add("li %s, 0x600", tmpReg);
    add("csrs mstatus, %s", tmpReg);
    add("li %s, 32", tmpReg);
    add("vsetvli %s, %s, e32,m1", tmpReg, tmpReg);
  }

  /**
   * Stores the contents of a vector register into memory at the specified address.
   *
   * <p>This method uses the RISC-V `vse32.v` instruction to store data from the provided
   * vector register into memory. A temporary general-purpose register is utilized
   * to hold the base address for the store operation.
   *
   * @param vec    The name of the vector register whose contents will be stored in memory.
   * @param addr   The memory address where the vector register contents will be stored.
   * @param tmpReg The name of the temporary general-purpose register used
   *               to store the base address.
   */
  public void storeVectorToMemory(String vec, long addr,
                                  String tmpReg) {
    add("li %s, 0x%x", tmpReg, addr);
    add("vse32.v  %s, (%s)", vec, tmpReg);
  }

  /**
   * Loads an array from memory into sequential general-purpose registers.
   *
   * <p>This method utilizes the RISC-V `lw` (load word) instruction to load
   * values from a contiguous memory region into a range of consecutive
   * general-purpose registers. A temporary register is used to hold the base
   * memory address during the load operations.
   *
   * @param addr     The starting memory address of the array to be loaded.
   * @param firstReg The first general-purpose register in the target range.
   * @param lastReg  The last general-purpose register in the target range.
   * @param tmpReg   The temporary register used to store the base memory address.
   */
  public void loadArrayToRegs(long addr, int firstReg,
                              int lastReg, String tmpReg) {
    add("li %s, 0x%x", tmpReg, addr);
    var regCount = lastReg - firstReg + 1;
    for (int i = 0; i < regCount; i++) {
      add("lw x%d, %d(%s)", i + firstReg, i * 4, tmpReg);
    }
  }

}
