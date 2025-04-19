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

package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.Type;

/**
 * Checks if the risc-v specifications are translated and optimized without errors.
 */
public class Rv3264imTest extends AbstractTest {

  void testRv32i(Specification spec) {
    var rv32i = TestUtils.findDefinitionByNameIn("RV32I", spec, InstructionSetArchitecture.class);
    TestUtils.findDefinitionByNameIn("RV3264I::ADD", spec, Instruction.class);

    assertThrows(AssertionFailedError.class,
        () -> TestUtils.findDefinitionByNameIn("RV3264IM::DIVU", spec, Instruction.class));
    Assertions.assertEquals(Type.bits(32),
        Objects.requireNonNull(rv32i.pc()).registerTensor().type());
  }

  void testRv32im(Specification spec) {
    var rv32im = TestUtils.findDefinitionByNameIn("RV32IM", spec, InstructionSetArchitecture.class);

    TestUtils.findDefinitionByNameIn("RV3264I::ADD", spec, Instruction.class);
    TestUtils.findDefinitionByNameIn("RV3264IM::DIVU", spec, Instruction.class);
    Assertions.assertEquals(Type.bits(32),
        Objects.requireNonNull(rv32im.pc()).registerTensor().type());
  }

  void testRv64i(Specification spec) {
    var rv64i = TestUtils.findDefinitionByNameIn("RV64I", spec, InstructionSetArchitecture.class);

    TestUtils.findDefinitionByNameIn("RV3264I::ADD", spec, Instruction.class);
    assertThrows(AssertionFailedError.class,
        () -> TestUtils.findDefinitionByNameIn("RV3264IM::DIVU", spec, Instruction.class));
    Assertions.assertEquals(Type.bits(64),
        Objects.requireNonNull(rv64i.pc()).registerTensor().type());
  }

  void testRv64im(Specification spec) {
    var rv64im = TestUtils.findDefinitionByNameIn("RV64IM", spec, InstructionSetArchitecture.class);

    TestUtils.findDefinitionByNameIn("RV3264I::ADD", spec, Instruction.class);
    TestUtils.findDefinitionByNameIn("RV3264IM::DIVU", spec, Instruction.class);
    Assertions.assertEquals(Type.bits(64),
        Objects.requireNonNull(rv64im.pc()).registerTensor().type());
  }


  Specification getSpecification(String rvFile) throws IOException, DuplicatedPassKeyException {
    // runs the general viam optimizations on the rv3264im impl
    //    var config = new GeneralConfiguration("build/test-out/rv3264im/", true);
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "sys/risc-v/" + rvFile,
        PassOrders.viam(config)
    );
    return setup.specification();
  }

  @TestFactory
  Stream<DynamicTest> test_all_riscv() {
    return Stream.of(
        dynamicTest("RV32I", () -> testRv32i(getSpecification("rv32i.vadl"))),
        dynamicTest("RV32IM", () -> testRv32im(getSpecification("rv32im.vadl"))),
        dynamicTest("RV64I", () -> testRv64i(getSpecification("rv64i.vadl"))),
        dynamicTest("RV64IM", () -> testRv64im(getSpecification("rv64im.vadl")))
    );
  }

}
