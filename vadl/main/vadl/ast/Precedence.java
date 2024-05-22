package vadl.ast;

enum Precedence implements Comparable<Precedence> {
  OR,
  AND,
  EQUALITY,
  COMPARISON,
  TERM,
  FACTOR;

  boolean smallerThan(Precedence other) {
    return this.ordinal() < other.ordinal();
  }

  boolean smallerEqualThan(Precedence other) {
    return this.ordinal() <= other.ordinal();
  }

  boolean greaterThan(Precedence other) {
    return this.ordinal() > other.ordinal();
  }

  boolean greaterEqualThan(Precedence other) {
    return this.ordinal() >= other.ordinal();
  }


  Precedence next() {
    return values()[ordinal() + 1];
  }
}
