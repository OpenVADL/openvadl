package vadl.lcb.passes.llvmLowering.model;

import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.viam.ViamError;

/**
 * LLVM Condition Codes for logic comparisons.
 */
public enum LlvmCondCode {
  SETEQ,
  SETNE,
  SETUGT,
  SETUGE,
  SETULE,
  SETGT,
  SETGE,
  SETLE,
  SETLT,
  SETULT;

  /**
   * Convert a {@link BuiltIn} into a {@link LlvmCondCode}.
   *
   * @return the converted CondCode or {@code null}.
   */
  public static LlvmCondCode from(BuiltIn built) {
    if (built == BuiltInTable.EQU) {
      return LlvmCondCode.SETEQ;
    } else if (built == BuiltInTable.NEQ) {
      return LlvmCondCode.SETNE;
    } else if (built == BuiltInTable.SGTH) {
      return LlvmCondCode.SETGT;
    } else if (built == BuiltInTable.UGTH) {
      return LlvmCondCode.SETUGT;
    } else if (built == BuiltInTable.SLTH) {
      return LlvmCondCode.SETLT;
    } else if (built == BuiltInTable.ULTH) {
      return LlvmCondCode.SETULT;
    } else if (built == BuiltInTable.SLEQ) {
      return LlvmCondCode.SETLE;
    } else if (built == BuiltInTable.ULEQ) {
      return LlvmCondCode.SETULE;
    } else if (built == BuiltInTable.SGEQ) {
      return LlvmCondCode.SETGE;
    } else if (built == BuiltInTable.UGEQ) {
      return LlvmCondCode.SETUGE;
    } else {
      throw new ViamError("not supported cond code");
    }
  }
}
