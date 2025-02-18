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
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;

class TrivialImmediateStrategyTest extends AbstractTest {
  TrivialImmediateStrategy strategy = new TrivialImmediateStrategy();

  @Test
  void shouldCreateEncoding_whenNoBuiltIns() {
    // Given
    var format = createFormat("formatValue", BitsType.bits(32));
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);

    // Setup behavior
    var returnNode =
        new ReturnNode(new TypeCastNode(new FieldRefNode(field, DataType.bits(20)),
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