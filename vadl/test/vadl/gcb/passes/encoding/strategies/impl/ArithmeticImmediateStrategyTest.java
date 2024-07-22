package vadl.gcb.passes.encoding.strategies.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
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
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.matching.impl.FuncParamMatcher;

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

  @ParameterizedTest
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

    var result =
        strategy.checkIfApplicable(new Format.FieldAccess(createIdentifier("identifierValue"),
            accessFunction,
            createFunction("encodingNameValue", new Parameter(createIdentifier("identifierValue"),
                DataType.unsignedInt(32)), DataType.unsignedInt(32)),
            createFunction("predicateNameValue", new Parameter(createIdentifier("identifierValue"),
                DataType.unsignedInt(32)), DataType.unsignedInt(32))));

    assertThat(result).isTrue();
  }

  @ParameterizedTest
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

    var result =
        strategy.checkIfApplicable(new Format.FieldAccess(createIdentifier("identifierValue"),
            accessFunction,
            createFunction("encodingNameValue", new Parameter(createIdentifier("identifierValue"),
                DataType.unsignedInt(32)), DataType.unsignedInt(32)),
            createFunction("predicateNameValue", new Parameter(createIdentifier("identifierValue"),
                DataType.unsignedInt(32)), DataType.unsignedInt(32))));

    assertThat(result).isFalse();
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
        new ReturnNode(new TypeCastNode(new BuiltInCall(BuiltInTable.SUB,
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
    fieldAccess.setEncoding(new Function(createIdentifier("encodingFunctionName"),
        new Parameter[]
            {createParameter("parameterValue", DataType.unsignedInt(32))},
        DataType.bits(20)));

    // When
    strategy.generateEncoding(
        new Parameter(createIdentifier("identifierValue"), DataType.unsignedInt(32)),
        fieldAccess);

    // Then
    assertNotNull(fieldAccess.encoding());
    assertNotNull(fieldAccess.encoding().behavior());
    assertEquals(fieldAccess.encoding().returnType(), DataType.bits(20));

    // Checks if the SUB remains and the FuncParameter does not have a NegatedNode as wrapper.
    // Note we check for "ADD" because the strategy inverts all the SUBs into ADDS
    var hasNotNegatedFuncParam = TreeMatcher.matches(fieldAccess.encoding().behavior().getNodes(),
        new BuiltInMatcher(BuiltInTable.ADD, List.of(
            new ConstantValueMatcher(
                new Constant.Value(BigInteger.valueOf(31), DataType.unsignedInt(32))
            ),
            new BuiltInMatcher(BuiltInTable.NEG,
                new BuiltInMatcher(BuiltInTable.NEG,
                    new FuncParamMatcher(DataType.unsignedInt(32))))
        )));

    assertThat(hasNotNegatedFuncParam).isNotEmpty();
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
    fieldAccess.setEncoding(new Function(createIdentifier("encodingFunctionName"),
        new Parameter[]
            {createParameter("parameterValue", DataType.unsignedInt(32))},
        DataType.bits(20)));

    // When
    strategy.generateEncoding(
        new Parameter(createIdentifier("identifierValue"), DataType.unsignedInt(32)),
        fieldAccess);

    // Then
    assertNotNull(fieldAccess.encoding());
    assertNotNull(fieldAccess.encoding().behavior());
    assertEquals(fieldAccess.encoding().returnType(), DataType.bits(20));

    // Checks whether the SUB has been inverted and a NegatedNode exists.
    var hasNegatedFuncParam = TreeMatcher.matches(fieldAccess.encoding().behavior().getNodes(),
        new BuiltInMatcher(BuiltInTable.SUB, List.of(
            new ConstantValueMatcher(
                new Constant.Value(BigInteger.valueOf(31), DataType.unsignedInt(32))
            ),
            new BuiltInMatcher(BuiltInTable.NEG,
                new FuncParamMatcher(DataType.unsignedInt(32)))
        )));

    assertThat(hasNegatedFuncParam).isNotEmpty();
  }
}