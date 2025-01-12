package vadl.utils;

public class StringBuilderUtils {

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
