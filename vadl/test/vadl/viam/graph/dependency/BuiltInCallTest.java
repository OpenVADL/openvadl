package vadl.viam.graph.dependency;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.types.BuiltInTable.ADD;
import static vadl.types.BuiltInTable.MUL;
import static vadl.types.BuiltInTable.MULS;
import static vadl.types.BuiltInTable.commutative;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.ViamError;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;

class BuiltInCallTest {

  private static final SIntType SIGNED_INT = Type.signedInt(32);
  private static final ConstantNode constantNode = new ConstantNode(Constant.Value.of(
      1, SIGNED_INT)
  );


  private static Stream<Arguments> getCanonicalizableBuiltin() {
    return commutative.stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("getCanonicalizableBuiltin")
  void canonicalize_shouldSortConstantLast(BuiltInTable.BuiltIn builtin) {
    var node = new BuiltInCall(builtin, new NodeList<>(
        new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32))),
        new FieldRefNode(null, DataType.unsignedInt(32))
    ), Type.unsignedInt(32));

    node = (BuiltInCall) node.canonical();

    assertThat(node.arguments().size()).isEqualTo(2);
    assertThat(node.arguments().get(0).getClass()).isEqualTo(FieldRefNode.class);
    assertThat(node.arguments().get(1).getClass()).isEqualTo(ConstantNode.class);
  }

  @Test
  void verifyState_shouldThrowException_whenNotEnoughArguments() {
    var operation = new BuiltInCall(BuiltInTable.ADD,
        new NodeList<>(List.of(
            new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(31)))
        )),
        DataType.unsignedInt(32));

    var throwable = assertThrows(ViamGraphError.class, operation::verifyState);
    assertEquals("Number of arguments must match", throwable.getContextlessMessage());
  }

  @Test
  void verifyState_shouldThrowException_whenResultDoesNotMatch() {
    var operation = new BuiltInCall(BuiltInTable.ADD,
        new NodeList<>(List.of(
            new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(31))),
            new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)))
        )),
        DataType.unsignedInt(32));

    var throwable = assertThrows(ViamGraphError.class, operation::verifyState);
    assertEquals("Result type does not match", throwable.getContextlessMessage());
  }

}