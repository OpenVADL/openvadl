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

package vadl.viam.passes;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.pass.PassOrders;
import vadl.types.BuiltInTable;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.pcOffset.PcOffsetPass;

public class PcOffsetTest extends AbstractTest {

  @TestFactory
  public Stream<DynamicTest> testOffsets() {
    return Stream.of(
        testPcSubCalls("valid_pc_alias_subcalls.vadl"),
        testPcSubCalls("valid_pc_alias_subcalls_with_current.vadl"),
        testPcSubCalls("valid_pc_alias_subcalls_with_next.vadl"),

        testPcSubCalls("valid_pc_subcalls.vadl"),
        testPcSubCalls("valid_pc_subcalls_with_current.vadl"),
        testPcSubCalls("valid_pc_subcalls_with_next.vadl"),

        testPcOffsetAnnotation("valid_pc_ann_current.vadl", 0),
        testPcOffsetAnnotation("valid_pc_ann_next.vadl", 1),
        testPcOffsetAnnotation("valid_pc_ann_nextnext.vadl", 2),

        testPcOffsetAnnotation("valid_pc_alias_ann_current.vadl", 0),
        testPcOffsetAnnotation("valid_pc_alias_ann_next.vadl", 1),
        testPcOffsetAnnotation("valid_pc_alias_ann_nextnext.vadl", 2)
    );
  }

  private DynamicTest testPcOffsetAnnotation(String fileName, int offset) {
    return dynamicTest(fileName, () -> {
      var spec = setupPassManagerAndRunSpec(
          "passes/pcOffset/" + fileName,
          PassOrders.viam(getConfiguration(false))
              .untilFirst(PcOffsetPass.class)
      ).specification();
      testPcOffset("PcTest::READ_PC", spec, offset);
    });
  }

  private DynamicTest testPcSubCalls(String fileName) {
    return dynamicTest(fileName, () -> {
      var spec = setupPassManagerAndRunSpec(
          "passes/pcOffset/" + fileName,
          PassOrders.viam(getConfiguration(false))
              .untilFirst(PcOffsetPass.class)
      ).specification();
      testPcOffset("PcTest::READ_PC_CURRENT", spec, 0);
      testPcOffset("PcTest::READ_PC_NEXT", spec, 1);
      testPcOffset("PcTest::READ_PC_NEXTNEXT", spec, 2);
    });
  }

  private void testPcOffset(String instrName, Specification spec, int offset) {
    var instr = TestUtils.findDefinitionByNameIn(instrName, spec, Instruction.class);
    var readReg = getSingleNode(instr.behavior(), ReadRegTensorNode.class);
    Assertions.assertTrue(readReg.hasUsages());
    var usage = readReg.usages().findFirst().get();
    if (offset == 0) {
      Assertions.assertInstanceOf(WriteResourceNode.class, usage);
    } else {
      Assertions.assertInstanceOf(BuiltInCall.class, usage);
      var add = (BuiltInCall) usage;
      Assertions.assertEquals(BuiltInTable.ADD, add.builtIn());
      var offsetNode = add.arg(1);
      Assertions.assertInstanceOf(ConstantNode.class, offsetNode);
      Assertions.assertEquals(offset * 4,
          ((ConstantNode) offsetNode).constant().asVal().intValue());
    }
  }
}
