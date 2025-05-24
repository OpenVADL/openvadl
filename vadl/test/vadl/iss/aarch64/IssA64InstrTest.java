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

package vadl.iss.aarch64;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.google.errorprone.annotations.concurrent.LazyInit;
import vadl.TestUtils;
import vadl.iss.AsmTestBuilder;
import vadl.iss.AutoAssembler;
import vadl.iss.IssTestUtils;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Disassembler;
import vadl.utils.ViamUtils;
import vadl.vdt.impl.theiling.TheilingDecodeTreeGenerator;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;

public class IssA64InstrTest extends AbstractIssAarch64InstrTest {

  private static final Logger log = LoggerFactory.getLogger(IssA64InstrTest.class);
  @LazyInit
  InstructionSetArchitecture isa;
  @LazyInit
  AutoAssembler autoAssembler;

  @Override
  public int getTestPerInstruction() {
    return 50;
  }

  @Override
  public String getVadlSpec() {
    return "sys/aarch64/virt.vadl";
  }

  @Override
  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new A64TestBuilder(testNamePrefix + id);
  }

  @BeforeEach
  void setup() throws IOException, DuplicatedPassKeyException {
    if (isa == null) {
      var setup =
          setupPassManagerAndRunSpec(getVadlSpec(), PassOrders.iss(getConfiguration(false)));
      isa = setup.specification().isa().get();
      var disassembler = new Disassembler(isa, new TheilingDecodeTreeGenerator());
      // TODO: include X31/SP (the 0 register or stack pointer)
      autoAssembler = new AutoAssembler(disassembler)
          // register x0,x1 are used for test termination
          // register x2 is used to test the nzcv register
          .allowRegisterIndices(3, 31);
    }
  }
  
  @TestFactory
  Stream<DynamicTest> testADC() throws IOException {
    return runTestsWith(makeTestCases("ADCW", "ADCX"));
  }

  @TestFactory
  Stream<DynamicTest> testADCS() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("ADCS"));
  }

  // TODO: check ADD

  @TestFactory
  Stream<DynamicTest> testADDI() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("ADDXI", "ADDWI"));
  }

  @TestFactory
  Stream<DynamicTest> testASR() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("ASRW", "ASRX"));
  }

  @TestFactory
  Stream<DynamicTest> testMOVK() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("MOVK"));
  }

  @Test
  void testAutoAssembler() {
    // just test that all instructions can be assembled
    for (var instr : isa.ownInstructions()) {
      System.out.println(autoAssembler.produce(instr).assembly());
    }
  }


  private List<Function<Integer, IssTestUtils.TestCase>> makeTestCasesFromPrefixes(
      String... instrPrefix) {
    var instrs = findInstrsWithPrefixes(instrPrefix).toArray(String[]::new);
    return makeTestCases(instrs);
  }

  private List<String> findInstrsWithPrefixes(String... instrPrefixes) {
    return ViamUtils.findDefinitionsByFilter(isa, d -> d instanceof Instruction instr
            && Arrays.stream(instrPrefixes)
            .anyMatch(prefix -> instr.identifier.name().startsWith("AArch64Base::" + prefix)))
        .stream().map(Definition::simpleName).toList();
  }

  private List<Function<Integer, IssTestUtils.TestCase>> makeTestCases(String... instrs) {
    return Stream.of(instrs)
        .map(instr -> (Function<Integer, IssTestUtils.TestCase>) (Integer integer) -> makeTestCase(
            instr, integer))
        .toList();
  }

  private IssTestUtils.TestCase makeTestCase(String instrName, int id) {
    var instr =
        TestUtils.findDefinitionByNameIn("AArch64Base::" + instrName, isa, Instruction.class);
    var result = autoAssembler.produce(instr);
    var builder = getBuilder(instrName + "_", id);
    for (var reg : result.srcRegs()) {
      var regIndex = result.assignment().get(reg);
      builder.fillReg("x" + regIndex, 64);
    }
    builder.add(result.assembly());
    builder.add("mrs x2, nzcv");
    return builder.toTestCase();
  }
}
