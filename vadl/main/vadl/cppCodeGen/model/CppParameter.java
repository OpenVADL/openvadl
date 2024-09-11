package vadl.cppCodeGen.model;

import vadl.viam.Identifier;
import vadl.viam.Parameter;

/**
 * Indicates that the parameter is a parameter for cpp.
 */
public class CppParameter extends Parameter {

  public CppParameter(Identifier identifier, CppType type) {
    super(identifier, type);
  }
}
