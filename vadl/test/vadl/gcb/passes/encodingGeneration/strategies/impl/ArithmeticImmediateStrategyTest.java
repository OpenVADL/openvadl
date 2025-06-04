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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static vadl.TestUtils.createField;
import static vadl.TestUtils.createFieldAccess;
import static vadl.TestUtils.createFormat;
import static vadl.TestUtils.createFunctionWithoutParam;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;

class ArithmeticImmediateStrategyTest extends AbstractTest {
  ArithmeticImmediateStrategy strategy = new ArithmeticImmediateStrategy();

  private static Stream<Arguments> allowedBuiltIns() {
    return Stream.of(Arguments.of(
        BuiltInTable.ADD,
        BuiltInTable.SUB
    ));
  }

  private static Stream<Arguments> notAllowedBuiltIns() {
    return Stream.of(Arguments.of(
        BuiltInTable.LSL
    ));
  }

  // TODO: Fix test
  // @ParameterizedTest
  @MethodSource("allowedBuiltIns")
  void checkIfApplicable_shouldReturnTrue(BuiltInTable.BuiltIn builtIn) {
    var format = createFormat("formatValue", BitsType.bits(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    accessFunction.behavior().addWithInputs(new BuiltInCall(builtIn, new NodeList<>() {
      // params required
    }, DataType.unsignedInt(32)));
    accessFunction.behavior().addWithInputs(new FieldRefNode(field, DataType.unsignedInt(20)));

//    var result =
//        strategy.checkIfApplicable(new Format.FieldAccess(createIdentifier("identifierValue"),
//            accessFunction,
//            createFunction("encodingNameValue", new Parameter(createIdentifier("identifierValue"),
//                DataType.unsignedInt(32)), DataType.unsignedInt(32)),
//            createFunction("predicateNameValue", new Parameter(createIdentifier("identifierValue"),
//                DataType.unsignedInt(32)), DataType.unsignedInt(32))));

//    assertThat(result).isTrue();
  }

  // TODO: Fix test
  // @ParameterizedTest
  @MethodSource("notAllowedBuiltIns")
  void checkIfApplicable_shouldReturnFalse(BuiltInTable.BuiltIn builtIn) {
    var format = createFormat("formatValue", BitsType.bits(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    accessFunction.behavior().addWithInputs(new BuiltInCall(builtIn, new NodeList<>() {
      // params required
    }, DataType.unsignedInt(32)));
    accessFunction.behavior().addWithInputs(new FieldRefNode(field, DataType.unsignedInt(20)));

//    var result =
//        strategy.checkIfApplicable(new Format.FieldAccess(createIdentifier("identifierValue"),
//            accessFunction,
//            createFunction("encodingNameValue", new Parameter(createIdentifier("identifierValue"),
//                DataType.unsignedInt(32)), DataType.unsignedInt(32)),
//            createFunction("predicateNameValue", new Parameter(createIdentifier("identifierValue"),
//                DataType.unsignedInt(32)), DataType.unsignedInt(32))));

//    assertThat(result).isFalse();
  }

  @Test
  void shouldCreateEncoding_whenSubInAccessFunction() {
    // Given
    var format = createFormat("formatValue", BitsType.bits(32));
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);

    // Setup behavior
    var returnNode =
        new ReturnNode(new BuiltInCall(BuiltInTable.SUB,
            new NodeList<>(
                new ConstantNode(Constant.Value.of(
                    31, DataType.unsignedInt(32))),
                new FieldRefNode(field, DataType.bits(20))
            ), Type.unsignedInt(32)));
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

    // Checks if the SUB remains and the FuncParameter does not have a NegatedNode as wrapper.
    // Note we check for "ADD" because the strategy inverts all the SUBs into ADDS
    var hasNotNegatedFieldAccessRef =
        TreeMatcher.matches(fieldAccess.encoding().behavior().getNodes(),
            new BuiltInMatcher(BuiltInTable.ADD, List.of(
                new ConstantValueMatcher(
                    Constant.Value.of(31, DataType.unsignedInt(32))
                ),
                new FieldAccessRefMatcher()))
        );

    assertThat(hasNotNegatedFieldAccessRef).isNotEmpty();
  }

  @Test
  void shouldCreateEncoding_whenAddInAccessFunction() {
    // Given
    var format = createFormat("formatValue", BitsType.bits(32));
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);

    // Setup behavior
    var returnNode =
        new ReturnNode(new BuiltInCall(BuiltInTable.ADD,
            new NodeList<>(
                new ConstantNode(Constant.Value.of(
                    31, DataType.unsignedInt(32))),
                new FieldRefNode(field, DataType.bits(20))
            ), Type.unsignedInt(32)));
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

    // Checks whether the SUB has been inverted and a NegatedNode exists.
    var hasNegatedFieldAccessRef = TreeMatcher.matches(fieldAccess.encoding().behavior().getNodes(),
        new BuiltInMatcher(BuiltInTable.SUB, List.of(
            new ConstantValueMatcher(
                Constant.Value.of(31, DataType.unsignedInt(32))
            ),
            new FieldAccessRefMatcher()
        )));

    assertThat(hasNegatedFieldAccessRef).isNotEmpty();
  }
}