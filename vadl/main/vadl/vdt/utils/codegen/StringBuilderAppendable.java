package vadl.vdt.utils.codegen;

/**
 * A simple implementation of {@link CodeGeneratorAppendable} that uses a {@link StringBuilder} to
 * store the generated code. This implementation is not thread-safe.
 */
public class StringBuilderAppendable implements CodeGeneratorAppendable {

  private int indentLevel = 0;
  private final String indentStr;
  private final StringBuilder sb = new StringBuilder();

  public StringBuilderAppendable() {
    this.indentStr = "  ";
  }

  public StringBuilderAppendable(String indentStr) {
    this.indentStr = indentStr;
  }

  @Override
  public CodeGeneratorAppendable append(CharSequence csq) {

    indentCorrectly();

    csq.codePoints().forEach(c -> {
      if (isNewLine(c)) {
        newLine();
        indentCorrectly();
      } else {
        sb.appendCodePoint(c);
      }
    });
    return this;
  }

  @Override
  public CodeGeneratorAppendable append(Object obj) {

    indentCorrectly();

    if (obj instanceof CharSequence) {
      append((CharSequence) obj);
      return this;
    } else if (obj instanceof Character c) {
      if (isNewLine(c)) {
        newLine();
      } else {
        sb.append((char) c);
      }
      return this;
    } else if (obj instanceof Integer) {
      sb.append((int) obj);
      return this;
    } else if (obj instanceof Long) {
      sb.append((long) obj);
      return this;
    } else if (obj instanceof Float) {
      sb.append((float) obj);
      return this;
    } else if (obj instanceof Double) {
      sb.append((double) obj);
      return this;
    } else if (obj instanceof Boolean) {
      sb.append((boolean) obj);
      return this;
    } else if (obj instanceof Byte) {
      sb.append((byte) obj);
      return this;
    } else if (obj instanceof Short) {
      sb.append((short) obj);
    }

    // At last, just append the string representation of the object
    append(String.valueOf(obj));
    return this;
  }

  @Override
  public CodeGeneratorAppendable appendLn(CharSequence csq) {
    append(csq);
    newLine();
    return this;
  }

  @Override
  public CodeGeneratorAppendable appendLn(Object obj) {
    append(obj);
    newLine();
    return this;
  }

  @Override
  public CodeGeneratorAppendable newLine() {
    sb.append("\n");
    indentCorrectly();
    return this;
  }

  @Override
  public CodeGeneratorAppendable indent() {
    indentLevel++;
    indentCorrectly();
    return this;
  }

  @Override
  public CodeGeneratorAppendable unindent() {
    if (indentLevel > 0) {
      indentLevel--;
    }
    indentCorrectly();
    return this;
  }

  /**
   * Indent the current line according to the current indentation level. If the line is empty or
   * only consists of leading whitespace, it is replaced with an empy line with the correct
   * indentation level.
   */
  private void indentCorrectly() {
    if (sb.isEmpty()) {
      sb.append(getIndentation());
      return;
    } else if (sb.codePointAt(sb.length() - 1) == '\n') {
      sb.append(getIndentation());
      return;
    }

    int i = sb.length() - 1;
    while (i >= 0 && Character.isWhitespace(sb.codePointAt(i)) && sb.codePointAt(i) != '\n') {
      i--;
    }

    if (i >= 0 && sb.codePointAt(i) == '\n') {
      sb.delete(i + 1, sb.length());
      sb.append(getIndentation());
    }
  }

  private boolean isNewLine(int codePoint) {
    return codePoint == '\n' || codePoint == '\r' || codePoint == '\f' || codePoint == '\u0085'
        || codePoint == '\u2028' || codePoint == '\u2029';
  }

  private CharSequence getIndentation() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indentLevel; i++) {
      sb.append(indentStr);
    }
    return sb;
  }

  @Override
  public CharSequence toCharSequence() {
    return sb;
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
