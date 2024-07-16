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
        Arguments.of(DataType.signedInt(8), "(char) 1"),
        Arguments.of(DataType.signedInt(16), "(short int) 1"),
        Arguments.of(DataType.signedInt(32), "(long int) 1"),
        Arguments.of(DataType.signedInt(64), "(long long int) 1"),
        Arguments.of(DataType.unsignedInt(8), "(unsigned char) 1"),
        Arguments.of(DataType.unsignedInt(16), "(unsigned short int) 1"),
        Arguments.of(DataType.unsignedInt(32), "(unsigned long int) 1"),
        Arguments.of(DataType.unsignedInt(64), "(unsigned long long int) 1")
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