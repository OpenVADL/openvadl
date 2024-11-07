package vadl.lcb.passes.llvmLowering.domain.selectionDag;

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

  /**
   * Convert a {@link LlvmCondCode} into a {@link BuiltIn}.
   *
   * @return the converted CondCode or {@code null}.
   */
  public static BuiltIn from(LlvmCondCode condCode) {
    if (condCode == LlvmCondCode.SETEQ) {
      return BuiltInTable.EQU;
    } else if (condCode == LlvmCondCode.SETNE) {
      return BuiltInTable.NEQ;
    } else if (condCode == LlvmCondCode.SETGT) {
      return BuiltInTable.SGTH;
    } else if (condCode == LlvmCondCode.SETUGT) {
      return BuiltInTable.UGTH;
    } else if (condCode == LlvmCondCode.SETLT) {
      return BuiltInTable.SLTH;
    } else if (condCode == LlvmCondCode.SETULT) {
      return BuiltInTable.ULTH;
    } else if (condCode == LlvmCondCode.SETLE) {
      return BuiltInTable.SLEQ;
    } else if (condCode == LlvmCondCode.SETULE) {
      return BuiltInTable.ULEQ;
    } else if (condCode == LlvmCondCode.SETGE) {
      return BuiltInTable.SGEQ;
    } else if (condCode == LlvmCondCode.SETUGE) {
      return BuiltInTable.UGEQ;
    } else {
      throw new ViamError("not supported cond code");
    }
  }

  /**
   * Get the inverse for the {@code condition}.
   */
  public static LlvmCondCode inverse(LlvmCondCode condition) {
    return switch (condition) {
      case SETEQ -> SETNE;
      case SETNE -> SETEQ;
      case SETUGT -> SETULT;
      case SETUGE -> SETULE;
      case SETULE -> SETUGE;
      case SETGT -> SETLT;
      case SETGE -> SETLE;
      case SETLE -> SETGE;
      case SETLT -> SETGT;
      case SETULT -> SETUGT;
    };
  }
}
