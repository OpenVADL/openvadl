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

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.AsmTestBuilder;
import vadl.iss.IssTestUtils;

public class IssRV32ZicsrInstrTest extends AbstractIssRiscv32InstrTest {
  private static final String VADL_SPEC = "sys/risc-v/rvcsr.vadl";
  private static final int TESTS_PER_INSTRUCTION = 50;

  @Override
  public int getTestPerInstruction() {
    return TESTS_PER_INSTRUCTION;
  }

  @Override
  public String getVadlSpec() {
    return VADL_SPEC;
  }

  @Override
  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMTestBuilder(testNamePrefix + "_" + id);
  }

  @TestFactory
  Stream<DynamicTest> testSimpleTrap() throws IOException {
    var asmCore = """
        la t0, handler
        csrw mtvec, t0
        
        li a2, 1
        li a0, 1
        li a1, 1
        ebreak
        li a0, 2
        j exit
        
        handler:
        csrr t1, mepc
        addi t1, t1, 4
        csrw mepc, t1
        addi t2, x0, 5
        csrr a3, mcause
        mret
        
        li a2, 2 # should not reach this 
        
        exit:
        """;
    return runTest(new IssTestUtils.TestCase("Simple Trap", asmCore));
  }


}
