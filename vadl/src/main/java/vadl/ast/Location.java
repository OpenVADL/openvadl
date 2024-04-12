package vadl.ast;

/**
 * Location describes a span inside a source code document.
 * The class is used for well formatted error messages.
 */
public class Location {
  // FIXME: Maybe we need a chain to other locations here if macros are expanded,
  // but that's a future problem.
  public String file;
  public int startLine;
  public int endLine;
  public int startColumn;
  public int endColumn;

  /**
   * Constructs a new location by all its properties.
   */
  public Location(String file, int startLine, int endLine, int startColumn, int endColumn) {
    this.file = file;
    this.startLine = startLine;
    this.endLine = endLine;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
  }

  /**
   * Creates a new Location by merging two existing ones.
   * The locations can be in any order and can be intersecting, they only need to be from the
   * same file.
   */
  public Location(Location a, Location b) {
    // FIXME: I don't think it should be allowed to merge locations from differnt files
    assert (a.file.equals(b.file));

    this.file = a.file;
    this.startLine = Math.min(a.startLine, b.startLine);
    this.endLine = Math.max(a.startLine, b.startLine);

    this.startColumn = a.startColumn;
    if (b.startLine < a.startLine
        || (b.startLine == a.startLine) && b.startColumn < a.startColumn) {
      this.startColumn = b.startColumn;
    }

    this.endColumn = b.endColumn;
    if (a.endLine > b.endLine || (a.endLine == b.endLine) && a.endColumn > b.endColumn) {
      this.endColumn = a.endColumn;
    }
  }
}
