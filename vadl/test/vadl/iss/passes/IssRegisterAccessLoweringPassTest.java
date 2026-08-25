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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static vadl.utils.GraphUtils.getNodes;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.configuration.IssConfiguration;
import vadl.error.DiagnosticList;
import vadl.iss.passes.common.IssRegisterAccessLoweringPass;
import vadl.iss.passes.nodes.IssStaticReadRegNode;
import vadl.pass.PassOrders;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.annotations.TbStateRegisterAnnotation;
import vadl.viam.graph.dependency.ReadRegTensorNode;

public class IssRegisterAccessLoweringPassTest extends AbstractTest {

  private static final Constant.BitSlice slice = new Constant.BitSlice(
      Constant.BitSlice.Part.of(31, 31),
      Constant.BitSlice.Part.of(1, 0)
  );

  @TestFactory
  Stream<DynamicTest> testTbState() {
    return Stream.of(
        regRead("tb_state_reg_read.vadl", null, true),
        regRead("tb_state_reg_format_read.vadl", null, true),
        regRead("tb_state_reg_format_slice_static_read.vadl", slice, true),
        regRead("tb_state_reg_format_slice_non_static_read.vadl", slice, false),
        regRead("tb_state_reg_format_slice_whole_non_static_read.vadl", slice, false)
    );
  }

  private DynamicTest regRead(String fileName, Constant.BitSlice slice, boolean staticRead) {
    return dynamicTest(fileName, () -> {
      var config = new IssConfiguration(getConfiguration(false));
      var spec = setupPassManagerAndRunSpec(
          "passes/issRegisterAccessLowering/" + fileName,
          PassOrders.iss(config).untilFirst(IssRegisterAccessLoweringPass.class)
      ).specification();
      var graph = TestUtils.findDefinitionByNameIn("Test::INSTR", spec, Instruction.class)
          .behavior();
      var reg = staticRead
          ? getSingleNode(graph, IssStaticReadRegNode.class).regTensor()
          : getSingleNode(graph, ReadRegTensorNode.class).regTensor();
      assertEquals(0, (staticRead
          ? getNodes(graph, ReadRegTensorNode.class)
          : getNodes(graph, IssStaticReadRegNode.class)).size()
      );
      var ann = reg.annotation(TbStateRegisterAnnotation.class);
      assertNotNull(ann);
      assertEquals(slice, ann.slice());
    });
  }
}
