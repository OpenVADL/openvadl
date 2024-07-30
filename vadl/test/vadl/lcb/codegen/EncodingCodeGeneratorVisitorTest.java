package vadl.lcb.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.oop.passes.type_normalization.UpcastedTypeCastNode;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.TypeCastNode;

class EncodingCodeGeneratorVisitorTest extends AbstractTest {
  StringWriter writer;
  EncodingCodeGeneratorVisitor visitor;

  @BeforeEach
  void beforeEach() {
    writer = new StringWriter();
    visitor = new EncodingCodeGeneratorVisitor(writer);
  }

  @Test
  void constant_shouldReturnNumber() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);

    // When
    visitor.visit(node);

    // Then
    assertEquals("1", writer.toString());
  }

  @Test
  void constant_shouldReturnString() {
    var constant = new Constant.Str("testValue");
    var node = new ConstantNode(constant);

    // When
    visitor.visit(node);

    assertEquals("testValue", writer.toString());
  }

  @Test
  void funcCallNode_shouldCreateFunctionalWithOneVar() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var funcCallNode =
        new FuncCallNode(new NodeList<>(node), new Function(createIdentifier("nameValue"),
            new Parameter[] {createParameter("parameterValue", DataType.unsignedInt(32))},
            DataType.unsignedInt(32)), DataType.unsignedInt(32));

    // When
    visitor.visit(funcCallNode);

    // Then
    assertEquals("nameValue(1)", writer.toString());
  }

  @Test
  void funcCallNode_shouldCreateFunctionalWithTwoVar() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var funcCallNode =
        new FuncCallNode(new NodeList<>(node, node), new Function(createIdentifier("nameValue"),
            new Parameter[] {createParameter("parameterValue", DataType.unsignedInt(32))},
            DataType.unsignedInt(32)), DataType.unsignedInt(32));

    // When
    visitor.visit(funcCallNode);

    // Then
    assertEquals("nameValue(1,1)", writer.toString());
  }

  private static Stream<Arguments> getTypesWithCastExpression() {
    return Stream.of(
        Arguments.of(DataType.bool(), "(bool) 1"),
        Arguments.of(DataType.signedInt(8), "(int8_t) 1"),
        Arguments.of(DataType.signedInt(16), "(int16_t) 1"),
        Arguments.of(DataType.signedInt(32), "(int32_t) 1"),
        Arguments.of(DataType.signedInt(64), "(int64_t) 1"),
        Arguments.of(DataType.signedInt(128), "(int128_t) 1"),
        Arguments.of(DataType.unsignedInt(8), "(uint8_t) 1"),
        Arguments.of(DataType.unsignedInt(16), "(uint16_t) 1"),
        Arguments.of(DataType.unsignedInt(32), "(uint32_t) 1"),
        Arguments.of(DataType.unsignedInt(64), "(uint64_t) 1"),
        Arguments.of(DataType.unsignedInt(128), "(uint128_t) 1")
    );
  }

  private static Stream<Arguments> getTypesWithCastExpressionAndBitMask() {
    return Stream.of(
        Arguments.of(DataType.bool(), "((uint128_t) 1) & 1"),
        Arguments.of(DataType.signedInt(8), "((uint128_t) 1) & (1U << 8) - 1"),
        Arguments.of(DataType.signedInt(16), "((uint128_t) 1) & (1U << 16) - 1"),
        Arguments.of(DataType.signedInt(32), "((uint128_t) 1) & (1U << 32) - 1"),
        Arguments.of(DataType.signedInt(64), "((uint128_t) 1) & (1U << 64) - 1"),
        Arguments.of(DataType.signedInt(128), "((uint128_t) 1) & (1U << 128) - 1"),
        Arguments.of(DataType.unsignedInt(8), "((uint128_t) 1) & (1U << 8) - 1"),
        Arguments.of(DataType.unsignedInt(16), "((uint128_t) 1) & (1U << 16) - 1"),
        Arguments.of(DataType.unsignedInt(32), "((uint128_t) 1) & (1U << 32) - 1"),
        Arguments.of(DataType.unsignedInt(64), "((uint128_t) 1) & (1U << 64) - 1"),
        Arguments.of(DataType.unsignedInt(128), "((uint128_t) 1) & (1U << 128) - 1")
    );
  }

  @ParameterizedTest
  @MethodSource("getTypesWithCastExpression")
  void typeCastNode_shouldGenerateCpp(DataType type, String expected) {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);

    // When
    visitor.visit(new TypeCastNode(node, type));

    // Then
    assertEquals(expected, writer.toString());
  }

  @ParameterizedTest
  @MethodSource("getTypesWithCastExpressionAndBitMask")
  void upcastedTypeCastNode_shouldGenerateCpp(DataType type, String expected) {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);

    // When
    // We cast to the largest bitWidth
    visitor.visit(new UpcastedTypeCastNode(node, DataType.unsignedInt(128), type));

    // Then
    assertEquals(expected, writer.toString());
  }


  @Test
  void builtIn_shouldGenerateCpp() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var builtIn =
        new BuiltInCall(BuiltInTable.ADD, new NodeList<>(node, node), DataType.unsignedInt(32));

    // When
    visitor.visit(builtIn);

    // Then
    assertEquals("1 + 1", writer.toString());
  }
}