package vadl.iss.passes.tcgLowering;

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

  public String cCode() {
    return "TCG_COND_" + this.name();
  }
}
