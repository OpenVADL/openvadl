package vadl.utils;

/**
 * Utility class for {@link StringBuilder}.
 */
public class StringBuilderUtils {

  private StringBuilderUtils() {
    // Utility class
  }

  /**
   * Joins the elements of the provided {@link Iterable} into a single {@link StringBuilder}
   * containing the provided delimiter between each element.
   *
   * @param delimiter The delimiter that separates each element.
   * @param elements  The elements to join together.
   * @return A {@link StringBuilder} containing the joined elements.
   */
  public static StringBuilder join(CharSequence delimiter,
                                   Iterable<? extends CharSequence> elements) {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (CharSequence element : elements) {
      if (first) {
        first = false;
      } else {
        sb.append(delimiter);
      }
      sb.append(element);
    }
    return sb;
  }

}
