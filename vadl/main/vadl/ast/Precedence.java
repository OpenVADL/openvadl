package vadl.ast;

enum Precedence implements Comparable<Precedence> {
  LOGICAL_OR,
  LOGICAL_AND,
  OR,
  XOR,
  AND,
  EQUALITY,
  COMPARISON,
  SHIFT,
  TERM,
  FACTOR;

  @SuppressWarnings("EnumOrdinal")
  boolean smallerThan(Precedence other) {
    return this.ordinal() < other.ordinal();
  }

  @SuppressWarnings("EnumOrdinal")
  boolean smallerEqualThan(Precedence other) {
    return this.ordinal() <= other.ordinal();
  }

  @SuppressWarnings("EnumOrdinal")
  boolean greaterThan(Precedence other) {
    return this.ordinal() > other.ordinal();
  }

  @SuppressWarnings("EnumOrdinal")
  boolean greaterEqualThan(Precedence other) {
    return this.ordinal() >= other.ordinal();
  }

  @SuppressWarnings("EnumOrdinal")
  Precedence next() {
    return values()[ordinal() + 1];
  }
}
