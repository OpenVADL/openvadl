package vadl.viam.matching.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.ConstantNode;

class ConstantValueMatcherTest {
  @Test
  void matches_shouldReturnTrue_whenConstantMatches() {
    var matcher = new ConstantValueMatcher(new Constant.Value.Value(BigInteger.ONE, DataType.unsignedInt(32)));
    var node = new ConstantNode(new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenConstantMismatches() {
    var matcher = new ConstantValueMatcher(new Constant.Value.Value(BigInteger.ZERO, DataType.unsignedInt(32)));
    var node = new ConstantNode(new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void matches_shouldReturnFalse_whenConstantTypeMismatches() {
    var matcher = new ConstantValueMatcher(new Constant.Value.Value(BigInteger.ONE, DataType.unsignedInt(31)));
    var node = new ConstantNode(new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }
}