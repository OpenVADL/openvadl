package vadl.viam.matching.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.DataType;
import vadl.viam.graph.dependency.FuncParamNode;

class FuncParamMatcherTest extends AbstractTest {
  @Test
  void matches_shouldReturnTrue_whenTypeMatches() {
    var matcher = new FuncParamMatcher(DataType.unsignedInt(32));
    var node = new FuncParamNode(createParameter("parameterValue", DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenTypeMismatches() {
    var matcher = new FuncParamMatcher(DataType.bool());
    var node = new FuncParamNode(createParameter("parameterValue", DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }

}