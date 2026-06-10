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

package vadl.iss.passes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static vadl.TestUtils.findDefinitionByNameIn;
import static vadl.utils.GraphUtils.branchEnd;
import static vadl.utils.GraphUtils.getNodes;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.configuration.IssConfiguration;
import vadl.error.DiagnosticList;
import vadl.iss.passes.common.IssApplyMemoryEndiannessPass;
import vadl.iss.passes.nodes.IssStaticEndianConditionNode;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Endianness;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.BranchBeginNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.WriteMemNode;

public class IssApplyMemoryEndiannessPassTest extends AbstractTest {

  @TestFactory
  Stream<DynamicTest> testEndian() {
    return Stream.of(
        invalid("invalid_bi_endian_condition_non_exec_state.vadl",
            "Endianness condition may only read bits from"),
        invalid("invalid_bi_endian_condition_non_exec_state_partial.vadl",
            "Endianness condition may only read bits from"),
        invalid("invalid_bi_endian_mem_region_read.vadl",
            "Value of register R is unknown"),

        readWithoutCond("big_endian_read_with_overwrite.vadl", Endianness.BIG),
        readWithoutCond("little_endian_read_with_overwrite.vadl", Endianness.LITTLE),

        writeWithoutCond("big_endian_write_with_overwrite.vadl", Endianness.BIG),
        writeWithoutCond("little_endian_write_with_overwrite.vadl", Endianness.LITTLE),

        readWithCond("bi_endian_read_with_cond.vadl"),
        writeWithCond("bi_endian_write_with_cond.vadl"),

        writeInMemRegion("bi_endian_mem_region_write_big.vadl", Endianness.BIG),
        writeInMemRegion("bi_endian_mem_region_write_little.vadl", Endianness.LITTLE)
    );
  }

  private DynamicTest invalid(String fileName, String diagSubstr) {
    return dynamicTest(fileName, () -> {
      var config = new IssConfiguration(getConfiguration(false));
      var diag = assertThrows(DiagnosticList.class, () -> setupPassManagerAndRunSpec(
          "passes/applyMemoryEndianness/" + fileName,
          PassOrders.iss(config).untilFirst(IssApplyMemoryEndiannessPass.class)
      ));
      assertEquals(1, diag.items.size());
      assertThat(diag.items.getFirst().getMessage(), containsString(diagSubstr));
    });
  }

  private DynamicTest writeInMemRegion(String fileName, Endianness endianness) {
    return dynamicTest(fileName, () -> {
      var processor = spec(fileName).processor().orElseGet(Assertions::fail);
      var regions = processor.memoryRegions();
      assertEquals(1, regions.size());
      var graph = regions.getFirst().behavior();
      // assert that the condition was not inserted and that only one write exists
      assertEquals(0, getNodes(graph, IfNode.class).size());
      checkEndianness(getSingleNode(graph, WriteMemNode.class), endianness);
    });
  }

  private DynamicTest readWithCond(String fileName) {
    return dynamicTest(fileName, () -> {
      var select = getSingleNode(getInstrGraph(fileName), SelectNode.class);
      assertInstanceOf(IssStaticEndianConditionNode.class, select.condition());
      checkEndianness(select.trueCase(), Endianness.BIG);
      checkEndianness(select.falseCase(), Endianness.LITTLE);
    });
  }

  private DynamicTest writeWithCond(String fileName) {
    return dynamicTest(fileName, () -> {
      var ifElse = getSingleNode(getInstrGraph(fileName), IfNode.class);
      assertInstanceOf(IssStaticEndianConditionNode.class, ifElse.condition());
      checkBranchEndianness(ifElse.trueBranch(), Endianness.BIG);
      checkBranchEndianness(ifElse.falseBranch(), Endianness.LITTLE);
    });
  }

  private void checkBranchEndianness(BranchBeginNode branch, Endianness endianness) {
    var sideEffects = branchEnd(branch).sideEffects();
    assertEquals(1, sideEffects.size());
    checkEndianness(sideEffects.getFirst(), endianness);
  }

  private void checkEndianness(Node memNode, Endianness endianness) {
    if (memNode instanceof ReadMemNode read) {
      assertEquals(endianness, read.endianness());
    } else if (memNode instanceof WriteMemNode write) {
      assertEquals(endianness, write.endianness());
    } else {
      fail();
    }
  }

  private DynamicTest readWithoutCond(String fileName, Endianness endianness) {
    return dynamicTest(fileName, () ->
        checkEndianness(getSingleNode(getInstrGraph(fileName), ReadMemNode.class), endianness));
  }

  private DynamicTest writeWithoutCond(String fileName, Endianness endianness) {
    return dynamicTest(fileName, () ->
        checkEndianness(getSingleNode(getInstrGraph(fileName), WriteMemNode.class), endianness));
  }

  private Graph getInstrGraph(String fileName) throws IOException, DuplicatedPassKeyException {
    return findDefinitionByNameIn("EndianTest::INSTR", spec(fileName), Instruction.class).behavior();
  }

  private Specification spec(String fileName) throws IOException, DuplicatedPassKeyException {
    var config = new IssConfiguration(getConfiguration(false));
    return setupPassManagerAndRunSpec(
        "passes/applyMemoryEndianness/" + fileName,
        PassOrders.iss(config).untilFirst(IssApplyMemoryEndiannessPass.class)
    ).specification();
  }

}
