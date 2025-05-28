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
import java.nio.ByteOrder;
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
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
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

  @TestFactory
  Stream<DynamicTest> testADC() throws IOException {
    return runTestsWith(makeTestCases("ADCW", "ADCX"));
  }

  @TestFactory
  Stream<DynamicTest> testADCS() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("ADCS"));
  }

  // TODO: ADD (extended register): Add extended and scaled register.

  @TestFactory
  Stream<DynamicTest> testADDImm() throws IOException {
    // ADD (immediate): Add immediate value.
    return runTestsWith(makeTestCasesFromPrefixes("ADDXI", "ADDWI"));
  }

  @TestFactory
  Stream<DynamicTest> testADDShiftedReg() throws IOException {
    // ADD (shifted register): Add optionally-shifted register.
    // TODO: Add W (sf == 0) variants of shifted register ("ADDWLSL", "ADDWLSR", "ADDWASR").
    //    This is currently not possible as the check
    //    `if sf == '0' && imm6<5> == '1' then EndOfDecode(Decode_UNDEF);` is not implemented yet.
    return runTestsWith(makeTestCases(
        "ADDW", "ADDX", "ADDXLSL", "ADDXLSR", "ADDXASR"
    ));
  }

  // TODO: ADDS (extended register): Add extended and scaled register.

  @TestFactory
  Stream<DynamicTest> testADDSImm() throws IOException {
    // ADD (immediate): Add immediate value.
    return runTestsWith(makeTestCasesFromPrefixes("ADDXSI", "ADDWSI"));
  }

  @TestFactory
  Stream<DynamicTest> testADDSShiftedReg() throws IOException {
    // ADD (shifted register): Add optionally-shifted register.
    // TODO: Add W (sf == 0) variants of shifted register ("ADDWSLSL", "ADDWSLSR", "ADDWSASR").
    //    This is currently not possible as the check
    //    `if sf == '0' && imm6<5> == '1' then EndOfDecode(Decode_UNDEF);` is not implemented yet.
    return runTestsWith(makeTestCases(
        "ADDWS", "ADDXS", "ADDXSLSL", "ADDXSLSR", "ADDXSASR"
    ));
  }

  @TestFactory
  Stream<DynamicTest> testASR() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("ASRW", "ASRX"));
  }

//  @TestFactory
//  Stream<DynamicTest> testCINC() throws IOException {
//    // CSINC: Conditional select increment.
//    return runTestsWith(makeTestCasesFromPrefixes("CINC"));
//  }
//
//  @TestFactory
//  Stream<DynamicTest> testCSINV() throws IOException {
//    // CSINV: Conditional select invert.
//    return runTestsWith(makeTestCasesFromPrefixes("CSINV"));
//  }
//
//  @TestFactory
//  Stream<DynamicTest> testCSNEG() throws IOException {
//    // CSNEG: Conditional select negation.
//    return runTestsWith(makeTestCasesFromPrefixes("CSNEG"));
//  }

  @TestFactory
  Stream<DynamicTest> testEXTR() throws IOException {
    // EXTR: Extract register.
    // TODO: EXTR W (sf == 0) variants ("EXTRW").
    //    This is currently not possible as the check
    //    `if sf == '0' && imms<5> == '1' then EndOfDecode(Decode_UNDEF);` is not implemented yet.
    return runTestsWith(makeTestCases("EXTRX"));
  }

  @TestFactory
  Stream<DynamicTest> testMOVK() throws IOException {
    return runTestsWith(makeTestCasesFromPrefixes("MOVK"));
  }


  // TODO: SUB (extended register): Subtract extended and scaled register.

  @TestFactory
  Stream<DynamicTest> testSUBImm() throws IOException {
    // SUB (immediate): Subtract immediate value.
    return runTestsWith(makeTestCasesFromPrefixes("SUBXI", "SUBWI"));
  }

  @TestFactory
  Stream<DynamicTest> testSUBShiftedReg() throws IOException {
    // SUB (shifted register): Subtract optionally-shifted register.
    // TODO: Add W (sf == 0) variants of shifted register ("SUBWLSL", "SUBWLSR", "SUBWASR").
    //    This is currently not possible as the check
    //    `if sf == '0' && imm6<5> == '1' then EndOfDecode(Decode_UNDEF);` is not implemented yet.
    return runTestsWith(makeTestCases(
        "SUBW", "SUBX", "SUBXLSL", "SUBXLSR", "SUBXASR"
    ));
  }


  // TODO: SUBS (extended register): Subtract extended and scaled register.

  @TestFactory
  Stream<DynamicTest> testSUBSImm() throws IOException {
    // SUB (immediate): Subtract immediate value.
    return runTestsWith(makeTestCasesFromPrefixes("SUBXSI", "SUBWSI"));
  }

  @TestFactory
  Stream<DynamicTest> testSUBSShiftedReg() throws IOException {
    // SUB (shifted register): Subtract optionally-shifted register.
    // TODO: Add W (sf == 0) variants of shifted register ("SUBWSLSL", "SUBWSLSR", "SUBWSASR").
    //    This is currently not possible as the check
    //    `if sf == '0' && imm6<5> == '1' then EndOfDecode(Decode_UNDEF);` is not implemented yet.
    return runTestsWith(makeTestCases(
        "SUBWS", "SUBXS", "SUBXSLSL", "SUBXSLSR", "SUBXSASR"
    ));
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
