package vadl.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

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

  public @Nullable String toSourceString() {
    try (Stream<String> lines = Files.lines(Paths.get(uri))) {
      if (begin.line <= 0) {
        return null;
      }

      var lineDiff = end.line - begin.line;
      var sourceLines = lines.skip(begin.line - 1).limit(lineDiff + 1)
          .collect(Collectors.toCollection(ArrayList::new));

      var lineNumber = sourceLines.size();
      return IntStream.range(0, lineNumber)
          .mapToObj(i -> {
            var line = sourceLines.get(i);
            if (i == lineNumber - 1 && end.column != -1) {
              line = line.substring(0, end.column - 1);
            }

            if (i == 0 && begin.column != -1) {
              line = line.substring(begin.column - 1);
            }
            return line;
          })
          .collect(Collectors.joining("\n"));

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
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
