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
}
