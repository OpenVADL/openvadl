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

package vadl.iss.aarch64;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
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
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IssA64InstrTest extends AbstractIssAarch64InstrTest {

  private static final Logger log = LoggerFactory.getLogger(IssA64InstrTest.class);
  @LazyInit
  InstructionSetArchitecture isa;
  @LazyInit
  AutoAssembler autoAssembler;

  HashSet<Instruction> testedInstrs = new HashSet<>();

  @Override
  public int getTestPerInstruction() {
    return 50;
  }

  @Override
  public String getVadlSpec() {
    return "sys/aarch64/vprocessor.vadl";
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
      // For auto-generating assembly code, endianness doesn't really matter, as long as assembler
      // and disassembler use the same encoding format.
      var byteOrder = ByteOrder.LITTLE_ENDIAN;
      var disassembler = new Disassembler(isa, new RegularDecodeTreeGenerator(), byteOrder);
      // TODO: include X31/SP (the 0 register or stack pointer)
      autoAssembler = new AutoAssembler(disassembler, byteOrder)
          // register x0,x1 are used for test termination
          // register x2 is used to test the nzcv register
          .allowRegisterIndices(3, 31);
    }
  }

  @Test
  void testAutoAssembler() throws IOException, DuplicatedPassKeyException {
    var a64Isa = setupPassManagerAndRunSpec("sys/aarch64/virt.vadl",
        PassOrders.iss(getConfiguration(false))
    ).specification().isa().get();
    
    // just test that all aarch64 base instructions can be assembled
    for (var instr : a64Isa.ownInstructions()) {
      System.out.println(autoAssembler.produce(instr).assembly());
    }
  }


  @Test
  @Order(1)
  void setupInstructionTests() throws IOException {
    initializeInstructionBatch(this::buildInstructionTestGroups);
  }

  @TestFactory
  @Order(2)
  Stream<DynamicNode> buildInstructionTests() throws IOException {
    initializeInstructionBatch(this::buildInstructionTestGroups);
    return buildInstructionTestContainers();
  }

  private List<InstructionTestGroup> buildInstructionTestGroups() {
    return List.of(
        instructionGroup("ADC", makeTestCases("ADCW", "ADCX")),
        instructionGroup("ADCS", makeTestCasesFromPrefixes("ADCS")),
        instructionGroup("ADD_EXT", makeTestCasesFromPrefixes("ADDWUX", "ADDWSX", "ADDXUX", "ADDXSX")),
        instructionGroup("ADD_IMM", makeTestCasesFromPrefixes("ADDXI", "ADDWI")),
        instructionGroup("ADD_SHIFTED_REG", makeTestCases(
            "ADDW", "ADDWLSL", "ADDWLSR", "ADDWASR", "ADDX", "ADDXLSL", "ADDXLSR", "ADDXASR")),
        instructionGroup("ADDS_EXT", makeTestCasesFromPrefixes("ADDWSUX", "ADDWSSX", "ADDXSUX", "ADDXSSX")),
        instructionGroup("ADDS_IMM", makeTestCasesFromPrefixes("ADDXSI", "ADDWSI")),
        instructionGroup("ADDS_SHIFTED_REG", makeTestCases(
            "ADDWS", "ADDWSLSL", "ADDWSLSR", "ADDWSASR", "ADDXS", "ADDXSLSL", "ADDXSLSR", "ADDXSASR")),
        instructionGroup("ANDS_SHIFTED", makeTestCasesFromPrefixes("ANDSW", "ANDSX")),
        instructionGroup("ASR", makeTestCasesFromPrefixes("ASRW", "ASRX")),
        instructionGroup("BICS", makeTestCasesFromPrefixes("BICS")),
        instructionGroup("CCMP", makeTestCasesFromPrefixes("CCMPI")),
        instructionGroup("CSINC", makeTestCasesFromPrefixes("CSINC")),
        instructionGroup("CSINV", makeTestCasesFromPrefixes("CSINV")),
        instructionGroup("CSNEG", makeTestCasesFromPrefixes("CSNEG")),
        instructionGroup("EXTR", makeTestCases("EXTRX", "EXTRW")),
        instructionGroup("MOVK", makeTestCasesFromPrefixes("MOVK")),
        instructionGroup("SBC", makeTestCases("SBCW", "SBCX")),
        instructionGroup("SBCS", makeTestCasesFromPrefixes("SBCS")),
        instructionGroup("SDIV", makeTestCasesFromPrefixes("SDIV")),
        instructionGroup("SUB_EXT", makeTestCasesFromPrefixes("SUBWUX", "SUBWSX", "SUBXUX", "SUBXSX")),
        instructionGroup("SUB_IMM", makeTestCasesFromPrefixes("SUBXI", "SUBWI")),
        instructionGroup("SUB_SHIFTED_REG", makeTestCases(
            "SUBW", "SUBWLSL", "SUBWLSR", "SUBWASR", "SUBX", "SUBXLSL", "SUBXLSR", "SUBXASR")),
        instructionGroup("SUBS_EXT", makeTestCasesFromPrefixes("SUBWSUX", "SUBWSSX", "SUBXSUX", "SUBXSSX")),
        instructionGroup("SUBS_IMM", makeTestCasesFromPrefixes("SUBXSI", "SUBWSI")),
        instructionGroup("SUBS_SHIFTED_REG", makeTestCases(
            "SUBWS", "SUBWSLSL", "SUBWSLSR", "SUBWSASR", "SUBXS", "SUBXSLSL", "SUBXSLSR", "SUBXSASR")),
        instructionGroup("UDIV", makeTestCasesFromPrefixes("UDIV")));
  }

  private InstructionTestGroup instructionGroup(String name,
                                                List<Function<Integer, IssTestUtils.TestCase>> generators) {
    return new InstructionTestGroup(name, buildTestsWith(generators));
  }

  private List<Function<Integer, IssTestUtils.TestCase>> makeTestCasesFromPrefixes(
      String... instrPrefix) {
    var instrs = findInstrsWithPrefixes(instrPrefix).toArray(String[]::new);
    return makeTestCases(instrs);
  }

  private List<String> findInstrsWithPrefixes(String... instrPrefixes) {
    var result = ViamUtils.findDefinitionsByFilter(isa, d -> d instanceof Instruction instr
            && Arrays.stream(instrPrefixes)
            .anyMatch(prefix -> instr.identifier.name().startsWith("AArch64Base::" + prefix)))
        .stream().map(Definition::simpleName).toList();
    if (result.isEmpty()) {
      throw new IllegalArgumentException(
          "No instructions found for " + Arrays.toString(instrPrefixes));
    }
    return result;
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

    if (testedInstrs.contains(instr)) {
      throw new IllegalStateException("Instruction was already tested: " + instr);
    }

    var result = autoAssembler.produce(instr);
    var builder = getBuilder(instrName + "_", id);

    // randomize set NZCV
    setRandomNZCV(builder);

    builder.add("# test body");
    // fill the source registers with data
    for (var reg : result.srcRegs()) {
      var regIndex = result.assignment().get(reg);
      if (regIndex.intValue() == 31) {
        // in the case of 31 we just don't set it
        continue;
      }
      var regName = "x" + regIndex;
      builder.fillRegUnsigned(regName, 64);
    }
    builder.add(result.assembly());
    builder.add("# end of test body");
    builder.add("mrs x2, nzcv");
    return builder.toTestCase();
  }

  private void setRandomNZCV(AsmTestBuilder builder) {
    var nzcv = TestUtils.arbitraryBits(4).sample();
    builder.add("# set NZCV");
    builder.add("mov x2, 0x%s", nzcv.toString(16));
    builder.add("lsl x2, x2, 28");
    builder.add("msr nzcv, x2");
  }

}
