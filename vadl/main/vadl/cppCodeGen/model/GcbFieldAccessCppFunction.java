package vadl.cppCodeGen.model;

import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;

/**
 * An extension of a {@link Function} which has more information about the generated code.
 */
public class GcbFieldAccessCppFunction extends Function {
  private final Format.FieldAccess fieldAccess;

  public GcbFieldAccessCppFunction(Identifier identifier,
                                   Parameter[] parameters,
                                   Type returnType,
                                   Graph behavior,
                                   Format.FieldAccess fieldAccess) {
    super(identifier, parameters, returnType, behavior);
    this.fieldAccess = fieldAccess;
  }

  public CppFunctionName functionName() {
    return new CppFunctionName(identifier);
  }

  public Format.FieldAccess fieldAccess() {
    return fieldAccess;
  }
}
