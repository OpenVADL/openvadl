package vadl.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class StreamUtilsTest {
  @Test
  public void directionalRangeAscendingOrder() {
    assertArrayEquals(new int[] {1, 2, 3, 4, 5}, StreamUtils.directionalRange(1, 6).toArray());
  }

  @Test
  public void directionalRangeDescendingOrder() {
    assertArrayEquals(new int[] {5, 4, 3, 2, 1}, StreamUtils.directionalRange(5, 0).toArray());
  }

  @Test
  public void directionalRangeSingleValue() {
    assertArrayEquals(new int[] {}, StreamUtils.directionalRange(0, 0).toArray());
  }

  @Test
  public void directionalRangeClosedAscendingOrder() {
    assertArrayEquals(new int[] {1, 2, 3, 4, 5},
        StreamUtils.directionalRangeClosed(1, 5).toArray());
  }

  @Test
  public void directionalRangeClosedDescendingOrder() {
    assertArrayEquals(new int[] {5, 4, 3, 2, 1},
        StreamUtils.directionalRangeClosed(5, 1).toArray());
  }

  @Test
  public void directionalRangeClosedSingleValue() {
    assertArrayEquals(new int[] {0}, StreamUtils.directionalRangeClosed(0, 0).toArray());
  }

}
