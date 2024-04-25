package vadl.utils;

import java.net.URI;

public record SourceLocation(
    URI uri,
    SourcePosition begin,
    SourcePosition end
) {

  public SourceLocation(URI uri, SourcePosition begin) {
    this(uri, begin, begin);
  }

  public SourceLocation(URI uri, int lineBegin, int lineEnd) {
    this(uri, new SourcePosition(lineBegin), new SourcePosition(lineEnd));
  }

  public SourceLocation(URI uri, int line) {
    this(uri, line, line);
  }

  public String toConciseString() {
    var uriAsString = this.uri.toString();
    var indexOfLastSlash = uriAsString.lastIndexOf('/');
    return uriAsString.substring(indexOfLastSlash + 1)
        + ":"
        + this.begin
        + " .. "
        + this.end;
  }

  @Override
  public String toString() {
    return uri + ":" + begin + ".." + end;
  }

  final
  public record SourcePosition(
      int line,
      int column
  ) {

    public SourcePosition(int line) {
      this(line, -1);
    }

    @Override
    public String toString() {
      if (column < 0) {
        return "" + line;
      }
      return line + ":" + column;
    }
  }
}
