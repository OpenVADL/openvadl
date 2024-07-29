package vadl.lcb.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.math.BigInteger;
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
  @MethodSource("getTypesWithCastExpressionAndBitMask")
  void upcastedTypeCastNode_shouldGenerateCpp(DataType type, String expected) {
    var constant = new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);

    // When
    // We cast to the largest bitWidth
    visitor.visit(new UpcastedTypeCastNode(node, DataType.unsignedInt(128), type));

    // Then
    assertEquals(expected, writer.toString());
  }
}