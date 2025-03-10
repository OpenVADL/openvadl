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

package vadl.gcb.passes.encodingGeneration.strategies.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static vadl.TestUtils.createField;
import static vadl.TestUtils.createFieldAccess;
import static vadl.TestUtils.createFormat;
import static vadl.TestUtils.createFunctionWithoutParam;

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SliceNode;

class ShiftedImmediateStrategyTest extends AbstractTest {
  ShiftedImmediateStrategy strategy = new ShiftedImmediateStrategy();

  @Test
  void shouldCreateEncoding_whenOnlyLeftShift() {
    // Given
    var format = createFormat("formatValue", BitsType.bits(32));
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);

    // Setup behavior
    var returnNode =
        new ReturnNode(new BuiltInCall(BuiltInTable.LSL,
            new NodeList<>(new FieldRefNode(field, DataType.bits(20)),
                new ConstantNode(Constant.Value.of(12, DataType.unsignedInt(32)))),
            Type.unsignedInt(32)));
    var startNode = new StartNode(returnNode);
    accessFunction.behavior().addWithInputs(returnNode);
    accessFunction.behavior().addWithInputs(startNode);

    var fieldAccess = createFieldAccess("fieldAccessValue",
        accessFunction);
    format.setFieldAccesses(new Format.FieldAccess[] {fieldAccess});

    // When
    strategy.generateEncoding(fieldAccess);

    // Then
    assertNotNull(fieldAccess.encoding());
    assertNotNull(fieldAccess.encoding().behavior());
    assertEquals(fieldAccess.encoding().returnType(), DataType.bits(20));
    assertThat(fieldAccess.encoding().behavior().getNodes())
        .anyMatch(
            x -> x.getClass() == SliceNode.class
                && ((SliceNode) x).type().equals(Type.bits(20)));

  }
}