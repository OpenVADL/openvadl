package vadl.cppCodeGen;

import static vadl.viam.ViamError.ensure;

import vadl.types.BuiltInTable;

/**
 * Not all Builtin's operators are valid cpp operands.
 */
public class BuiltInTranslator {
  /**
   * Map a builtin to a string.
   */
  public static String map(BuiltInTable.BuiltIn built) {
    if (built == BuiltInTable.EQU) {
      return "==";
    }

    ensure(built.operator() != null, "operator must be null");
    return built.operator();
  }
}
