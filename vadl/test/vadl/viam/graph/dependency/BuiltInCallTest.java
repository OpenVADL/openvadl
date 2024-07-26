package vadl.viam.graph.dependency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;
import jdk.jshell.spi.ExecutionControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.oop.SymbolTable;
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
  private static final ConstantNode constantNode = new ConstantNode(new Constant.Value(
      BigInteger.ONE, SIGNED_INT)
  );

  private static BuiltInCall getBuiltInCall(BuiltInTable.BuiltIn built) {
    return new BuiltInCall(built,
        new NodeList<>(constantNode, constantNode), SIGNED_INT);
  }

  private static ConstantNode wrapConstant(BigInteger bigInteger) {
    return new ConstantNode(new Constant.Value(bigInteger, SIGNED_INT));
  }

  public static Stream<Arguments> createNormalizeTestCases() {
    return Stream.of(
        Arguments.of(
            getBuiltInCall(BuiltInTable.ADD),
            wrapConstant(BigInteger.TWO)),
        Arguments.of(
            getBuiltInCall(BuiltInTable.SUB),
            wrapConstant(BigInteger.ZERO)),
        Arguments.of(
            getBuiltInCall(BuiltInTable.MUL),
            wrapConstant(BigInteger.ONE)),
        Arguments.of(
            getBuiltInCall(BuiltInTable.MULS),
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