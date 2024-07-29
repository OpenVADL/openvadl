package vadl.viam.matching.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.ConstantNode;

class AnyConstantValueMatcherTest extends AbstractTest {

  @Test
  void matches_shouldReturnTrue_whenConstantValue() {
    var matcher = new AnyConstantValueMatcher();
    var node = new ConstantNode(Constant.Value.of(0, DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenConstantBitSlice() {
    var matcher = new AnyConstantValueMatcher();
    var node = new ConstantNode(
        new Constant.BitSlice(new Constant.BitSlice.Part[] {
            new Constant.BitSlice.Part(0, 0)
        }));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }


  @Test
  void matches_shouldReturnFalse_whenConstantString() {
    var matcher = new AnyConstantValueMatcher();
    var node = new ConstantNode(new Constant.Str(""));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }
}