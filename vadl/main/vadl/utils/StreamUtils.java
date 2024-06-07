package vadl.utils;

import java.util.stream.IntStream;

/**
 * Utility class for creating and manipulating IntStreams.
 */
public class StreamUtils {

  /**
   * Creates an IntStream that goes from startInclusive to endExclusive, either in ascending or descending order.
   * If startInclusive is less than or equal to endExclusive, the stream is in ascending order.
   * Otherwise, the stream is in descending order.
   *
   * @param startInclusive the first element to be included in the stream
   * @param endExclusive   the first element to be excluded from the stream
   * @return an IntStream from startInclusive to endExclusive
   */
  public static IntStream directionalRange(int startInclusive, int endExclusive) {
    if (startInclusive <= endExclusive) {
      return IntStream.range(startInclusive, endExclusive);
    } else {
      return IntStream.iterate(startInclusive, i -> i > endExclusive, i -> i - 1);
    }
  }

  /**
   * Creates an IntStream that goes from startInclusive to endInclusive, either in ascending or descending order.
   * If startInclusive is less than or equal to endInclusive, the stream is in ascending order.
   * Otherwise, the stream is in descending order.
   *
   * @param startInclusive the first element to be included in the stream
   * @param endInclusive   the last element to be included in the stream
   * @return an IntStream from startInclusive to endInclusive
   */
  public static IntStream directionalRangeClosed(int startInclusive, int endInclusive) {
    if (startInclusive <= endInclusive) {
      return directionalRange(startInclusive, endInclusive + 1);
    } else {
      return IntStream.iterate(startInclusive, i -> i > endInclusive - 1, i -> i - 1);
    }
  }

}
