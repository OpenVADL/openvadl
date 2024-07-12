package vadl.gcb.passes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.matching.impl.FuncParamMatcher;

class GenerateFieldAccessEncodingFunctionPassTest extends AbstractTest {
  @Test
  void shouldCreateEncoding_whenNoBuiltIns() {
    // Given
    var spec = createSpecification("specificationNameValue");
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
    var instruction = createInstruction("instructionValue", format);
    spec.add(
        new InstructionSetArchitecture(createIdentifier("isaValue"),
            spec,
            Collections.emptyList(),
            List.of(instruction),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    // When
    var pass = new GenerateFieldAccessEncodingFunctionPass();
    pass.execute(Collections.emptyMap(), spec);

    // Then
    assertNotNull(fieldAccess.encoding());
    assertNotNull(fieldAccess.encoding().behavior());
    assertEquals(fieldAccess.encoding().returnType(), DataType.bits(20));
    assertThat(fieldAccess.encoding().behavior().getNodes())
        .anyMatch(
            x -> x.getClass() == SliceNode.class
                && ((SliceNode) x).type().equals(Type.bits(20)));

  }

  @Test
  void shouldCreateEncoding_whenOnlyLeftShift() {
    // Given
    var spec = createSpecification("specificationNameValue");
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
    var instruction = createInstruction("instructionValue", format);
    spec.add(
        new InstructionSetArchitecture(createIdentifier("isaValue"),
            spec,
            Collections.emptyList(),
            List.of(instruction),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    // When
    var pass = new GenerateFieldAccessEncodingFunctionPass();
    pass.execute(Collections.emptyMap(), spec);

    // Then
    assertNotNull(fieldAccess.encoding());
    assertNotNull(fieldAccess.encoding().behavior());
    assertEquals(fieldAccess.encoding().returnType(), DataType.bits(20));
    assertThat(fieldAccess.encoding().behavior().getNodes())
        .anyMatch(
            x -> x.getClass() == SliceNode.class
                && ((SliceNode) x).type().equals(Type.bits(20)));

  }

  @Test
  void shouldCreateEncoding_whenAddInAccessFunction() {
    // Given
    var spec = createSpecification("specificationNameValue");
    var format = createFormat("formatValue", BitsType.bits(32));
    var accessFunction = createFunctionWithoutParam("functionValue", DataType.unsignedInt(32));
    var field = createField("fieldValue",
        DataType.bits(20), new Constant.BitSlice(
            new Constant.BitSlice.Part[] {Constant.BitSlice.Part.of(19, 0)}), format);

    // Setup behavior
    var returnNode =
        new ReturnNode(new TypeCastNode(new BuiltInCall(BuiltInTable.ADD,
            new NodeList<>(
                new ConstantNode(new Constant.Value(
                    BigInteger.valueOf(31), DataType.unsignedInt(32))),
                new FieldRefNode(field, DataType.bits(20))
            ), Type.unsignedInt(32)),
            Type.unsignedInt(32)));
    var startNode = new StartNode(returnNode);
    accessFunction.behavior().addWithInputs(returnNode);
    accessFunction.behavior().addWithInputs(startNode);

    var fieldAccess = createFieldAccess("fieldAccessValue",
        accessFunction);
    format.setFieldAccesses(new Format.FieldAccess[] {fieldAccess});
    var instruction = createInstruction("instructionValue", format);
    spec.add(
        new InstructionSetArchitecture(createIdentifier("isaValue"),
            spec,
            Collections.emptyList(),
            List.of(instruction),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    // When
    var pass = new GenerateFieldAccessEncodingFunctionPass();
    pass.execute(Collections.emptyMap(), spec);

    // Then
    assertNotNull(fieldAccess.encoding());
    assertNotNull(fieldAccess.encoding().behavior());
    assertEquals(fieldAccess.encoding().returnType(), DataType.bits(20));

    var hasNegatedFuncParam = TreeMatcher.matches(fieldAccess.encoding().behavior().getNodes(),
        new BuiltInMatcher(BuiltInTable.SUB, List.of(
            new ConstantValueMatcher(
                new Constant.Value(BigInteger.ZERO, DataType.unsignedInt(32))
            ),
            new FuncParamMatcher(DataType.unsignedInt(32))
        )));

    assertThat(hasNegatedFuncParam).isNotEmpty();

    var hasInvertedAdd = TreeMatcher.matches(fieldAccess.encoding().behavior().getNodes(),
        new BuiltInMatcher(BuiltInTable.SUB, List.of(
            new ConstantValueMatcher(
                new Constant.Value(new BigInteger(String.valueOf(-31)), DataType.signedInt(32))),
            new BuiltInMatcher(BuiltInTable.SUB, Collections.emptyList())
        )));

    assertThat(hasInvertedAdd).isNotEmpty();
  }
}