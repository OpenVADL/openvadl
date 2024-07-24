package vadl.gcb.passes.encoding.strategies.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.gcb.passes.encoding.GenerateFieldAccessEncodingFunctionPass;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;

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
        new ReturnNode(new TypeCastNode(new BuiltInCall(BuiltInTable.LSL,
            new NodeList<>(new FieldRefNode(field, DataType.bits(20)),
                new ConstantNode(new Constant.Value(
                    BigInteger.valueOf(12), DataType.unsignedInt(32)))),
            Type.unsignedInt(32)),
            Type.bits(32)));
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