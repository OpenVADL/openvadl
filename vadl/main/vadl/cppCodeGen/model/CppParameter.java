package vadl.cppCodeGen.model;

import vadl.viam.Identifier;
import vadl.viam.Parameter;

public class CppParameter extends Parameter {

  public CppParameter(Identifier identifier, CppType type) {
    super(identifier, type);
  }
}
