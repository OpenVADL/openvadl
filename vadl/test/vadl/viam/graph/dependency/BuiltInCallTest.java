package vadl.viam.graph.dependency;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.types.BuiltInTable.ADD;
import static vadl.types.BuiltInTable.MUL;
import static vadl.types.BuiltInTable.MULS;
import static vadl.types.BuiltInTable.commutative;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;

class BuiltInCallTest {

  private static final SIntType SIGNED_INT = Type.signedInt(32);
  private static final ConstantNode constantNode = new ConstantNode(Constant.Value.of(
      BigInteger.ONE, SIGNED_INT)
  );

  private static BuiltInCall getBuiltInCall(BuiltInTable.BuiltIn built) {
    return new BuiltInCall(built,
        new NodeList<>(constantNode, constantNode), SIGNED_INT);
  }

  private static ConstantNode wrapConstant(BigInteger bigInteger) {
    return new ConstantNode(Constant.Value.of(bigInteger, SIGNED_INT));
  }

  public static Stream<Arguments> createNormalizeTestCases() {
    return Stream.of(
        Arguments.of(
            getBuiltInCall(ADD),
            wrapConstant(BigInteger.TWO)),
        Arguments.of(
            getBuiltInCall(BuiltInTable.SUB),
            wrapConstant(BigInteger.ZERO)),
        Arguments.of(
            getBuiltInCall(MUL),
            wrapConstant(BigInteger.ONE)),
        Arguments.of(
            getBuiltInCall(MULS),
            wrapConstant(BigInteger.ONE)),
        Arguments.of(
            getBuiltInCall(BuiltInTable.LSL),
            wrapConstant(BigInteger.TWO)),
        Arguments.of(
            getBuiltInCall(BuiltInTable.LSR),
            wrapConstant(BigInteger.ZERO))
    );
  }

  @ParameterizedTest
  @MethodSource("createNormalizeTestCases")
  void normalize_shouldReturnConstant(BuiltInCall origin, ConstantNode expected) {
    var result = origin.normalize();

    assertTrue(result.isPresent());
    var value = (Constant.Value) expected.constant();
    assertEquals(value.value(),
        ((Constant.Value) ((ConstantNode) result.get()).constant()).value());
  }

  private static Stream<Arguments> getCanonicalizableBuiltin() {
    return commutative.stream().map(Arguments::of);
  }

  private static Stream<Arguments> getNotCanonicalizBuiltin() {
    var notSupported = BuiltInTable.builtIns().collect(Collectors.toSet());
    notSupported.removeAll(commutative);

    return notSupported.stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("getCanonicalizableBuiltin")
  void canonicalize_shouldSortConstantLast(BuiltInTable.BuiltIn builtin) {
    var node = new BuiltInCall(builtin, new NodeList<>(
        new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32))),
        new FieldRefNode(null, DataType.unsignedInt(32))
    ), Type.unsignedInt(32));

    node.canonicalize();

    assertThat(node.arguments().size()).isEqualTo(2);
    assertThat(node.arguments().get(0).getClass()).isEqualTo(FieldRefNode.class);
    assertThat(node.arguments().get(1).getClass()).isEqualTo(ConstantNode.class);
  }

  @ParameterizedTest
  @MethodSource("getCanonicalizableBuiltin")
  void canonicalize_shouldSortConstantAscending_whenSameType(BuiltInTable.BuiltIn builtin) {
    var node = new BuiltInCall(builtin, new NodeList<>(
        new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32))),
        new ConstantNode(Constant.Value.of(BigInteger.TWO, DataType.unsignedInt(32)))
    ), Type.unsignedInt(32));

    node.canonicalize();

    assertThat(node.arguments().size()).isEqualTo(2);
    assertThat(node.arguments().get(0).getClass()).isEqualTo(ConstantNode.class);
    assertThat(((Constant.Value) ((ConstantNode) node.arguments().get(0)).constant()).value()
        .intValue()).isEqualTo(1);
    assertThat(((Constant.Value) ((ConstantNode) node.arguments().get(1)).constant()).value()
        .intValue()).isEqualTo(2);
  }


  @ParameterizedTest
  @MethodSource("getCanonicalizableBuiltin")
  void canonicalize_shouldNotSwap_whenWrappedByTypedCastNode(BuiltInTable.BuiltIn builtin) {
    var node = new BuiltInCall(builtin, new NodeList<>(
        new TypeCastNode(
            new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32))),
            Type.unsignedInt(32)),
        new FieldRefNode(null, DataType.unsignedInt(32))
    ), Type.unsignedInt(32));

    node.canonicalize();

    assertThat(node.arguments().size()).isEqualTo(2);
    assertThat(node.arguments().get(0).getClass()).isEqualTo(TypeCastNode.class);
    assertThat(node.arguments().get(1).getClass()).isEqualTo(FieldRefNode.class);
  }

  @ParameterizedTest
  @MethodSource("getNotCanonicalizBuiltin")
  void canonicalize_shouldNotSortConstantLast(BuiltInTable.BuiltIn builtin) {
    var node = new BuiltInCall(builtin, new NodeList<>(
        new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32))),
        new FieldRefNode(null, DataType.unsignedInt(32))
    ), Type.unsignedInt(32));

    node.canonicalize();

    assertThat(node.arguments().size()).isEqualTo(2);
    assertThat(node.arguments().get(0).getClass()).isEqualTo(ConstantNode.class);
    assertThat(node.arguments().get(1).getClass()).isEqualTo(FieldRefNode.class);
  }
}