package vadl.viam.graph.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.types.DataType;
import vadl.viam.Constant;

class TypeCastNodeTest {
  public static Stream<Arguments> getTypes() {
    return Stream.of(
        Arguments.of(DataType.bool(), "(bool) 1"),
        Arguments.of(DataType.signedInt(8), "(int8_t) 1"),
        Arguments.of(DataType.signedInt(16), "(int16_t) 1"),
        Arguments.of(DataType.signedInt(32), "(int32_t) 1"),
        Arguments.of(DataType.signedInt(64), "(int64_t) 1"),
        Arguments.of(DataType.unsignedInt(8), "(uint8_t) 1"),
        Arguments.of(DataType.unsignedInt(16), "(uint16_t) 1"),
        Arguments.of(DataType.unsignedInt(32), "(uint32_t) 1"),
        Arguments.of(DataType.unsignedInt(64), "(uint64_t) 1")
    );
  }

  @ParameterizedTest
  @MethodSource("getTypes")
  void shouldGenerateOopExpression(DataType type, String expected) {
    var constant = new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    assertEquals(expected, new TypeCastNode(node, type).generateOopExpression());
  }
}