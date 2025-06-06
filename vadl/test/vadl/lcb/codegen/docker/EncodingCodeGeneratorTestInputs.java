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

package vadl.lcb.codegen.docker;

import static vadl.TestUtils.createFieldWithParent;
import static vadl.TestUtils.createFunction;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import vadl.AbstractTest;
import vadl.gcb.passes.encodingGeneration.strategies.impl.ArithmeticImmediateStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.ShiftedImmediateStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.TrivialImmediateStrategy;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.helper.TestGraph;

public class EncodingCodeGeneratorTestInputs extends AbstractTest {
  public static Stream<Arguments> createFieldAccessFunctions() {
    return Stream.of(
        Arguments.of(createUnsignedInt32DecodingFunction(), new TrivialImmediateStrategy()),
        Arguments.of(createSignedInt32DecodingFunction(), new TrivialImmediateStrategy()),
        Arguments.of(createUnsignedInt32ShiftDecodingFunction(), new ShiftedImmediateStrategy()),
        Arguments.of(createSignedInt32ShiftDecodingFunction(), new ShiftedImmediateStrategy()),
        Arguments.of(createUnsignedInt32WithAdditionDecodingFunction(),
            new ArithmeticImmediateStrategy()),
        Arguments.of(createSignedInt32WithAdditionDecodingFunction(),
            new ArithmeticImmediateStrategy()),
        Arguments.of(createUnsignedInt32WithSubtractionDecodingFunction(),
            new ArithmeticImmediateStrategy()),
        Arguments.of(createSignedInt32WithSubtractionDecodingFunction(),
            new ArithmeticImmediateStrategy())
    );
  }

  private static Function createUnsignedInt32DecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new ZeroExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)));
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32DecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new SignExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)));
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createUnsignedInt32ShiftDecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.LSL, new NodeList<>(
            new ZeroExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)),
            new ConstantNode(
                Constant.Value.of(6, DataType.unsignedInt(32)))),
            Type.unsignedInt(32))
    );
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32ShiftDecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.LSL, new NodeList<>(
            new SignExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)),
            new ConstantNode(
                Constant.Value.of(6, DataType.unsignedInt(32)))),
            Type.signedInt(32))
    );
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createUnsignedInt32WithAdditionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
            new ZeroExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)),
            new ConstantNode(
                Constant.Value.of(6, DataType.unsignedInt(32)))),
            Type.unsignedInt(32))
    );
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32WithAdditionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
            new SignExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)),
            new ConstantNode(Constant.Value.of(6, DataType.signedInt(32)))),
            Type.signedInt(32))
    );
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createUnsignedInt32WithSubtractionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.SUB, new NodeList<>(
            new ZeroExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)),
            new ConstantNode(
                Constant.Value.of(6, DataType.unsignedInt(32)))),
            Type.unsignedInt(32))
    );
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32WithSubtractionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.SUB, new NodeList<>(
            new SignExtendNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)),
            new ConstantNode(Constant.Value.of(6, DataType.signedInt(32)))),
            Type.signedInt(32))
    );
    var graph = new TestGraph();
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }
}
