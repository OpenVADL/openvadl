package vadl.cppCodeGen.model;

import vadl.viam.Identifier;

public record CppFunctionName(Identifier identifier) {
  public String lower() {
    return identifier.lower();
  }
}
