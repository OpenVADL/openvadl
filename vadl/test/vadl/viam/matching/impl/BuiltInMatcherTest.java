package vadl.viam.matching.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;

class BuiltInMatcherTest {
  @Test
  void matches_shouldReturnTrue_whenInputsMatchExactly() {
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, List.of(
        new AnyConstantValueMatcher(),
        new AnyConstantValueMatcher()
    ));
    var input1 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnTrue_whenInputsNotMatchExactly() {
    // Here we specify only one matcher,
    // even though the node has two inputs.
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, List.of(
        new AnyConstantValueMatcher()
    ));
    var input1 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnTrue_whenNoMatchers() {
    // Here we specify no matchers,
    // even though the node has two inputs.
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, Collections.emptyList());
    var input1 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenBuiltInDoesNotMatch() {
    var matcher = new BuiltInMatcher(BuiltInTable.SUB, Collections.emptyList());
    var input1 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void matches_shouldReturnTrue_whenCommutativeAndInputs() {
    // Here we specify no matchers,
    // even though the node has two inputs.
    var matcher = new BuiltInMatcher(BuiltInTable.ADD, List.of(
        new ConstantValueMatcher(Constant.Value.of(BigInteger.ZERO, DataType.unsignedInt(32))),
        new ConstantValueMatcher(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)))
    ));
    var input1 = new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.unsignedInt(32)));
    var input2 = new ConstantNode(Constant.Value.of(BigInteger.ZERO, DataType.unsignedInt(32)));
    var operation = new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        input1, input2
    ), Type.unsignedInt(32));

    // When
    var result = matcher.matches(operation);

    // Then
    assertThat(result).isTrue();
  }
}