package vadl.iss.passes.tcgLowering;

/**
 * An enumeration defining various types of comparison conditions for TCG (Tiny Code Generator).
 * This enum provides a set of logical and comparison operators that can be used to generate
 * condition checks in TCG.
 */
public enum TcgCondition {
  EQ,
  NE,
  LT,
  GE,
  LE,
  GT,
  LTU,
  GEU,
  LEU,
  GTU,
  TSTEQ,
  TSTNE;

  @SuppressWarnings("MethodName")
  public String cCode() {
    return "TCG_COND_" + this.name();
  }

  public TcgCondition not() {
    return switch (this) {
      case EQ -> NE;
      case NE -> EQ;
      case LT -> GE;
      case GE -> LT;
      case LE -> GT;
      case GT -> LE;
      case LTU -> GEU;
      case GEU -> LTU;
      case LEU -> GTU;
      case GTU -> LEU;
      case TSTEQ -> TSTNE;
      case TSTNE -> TSTEQ;
    };
  }
}
