package vadl.cppCodeGen.model;

import vadl.viam.Identifier;

/**
 * Value wrapper for function name.
 */
public record CppFunctionName(Identifier identifier) {
  public String lower() {
    return identifier.lower();
  }
}
