package vadl.cppCodeGen.model;

import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;

/**
 * An extension of a {@link Function} which has more information about the generated code.
 */
public class CppFunction extends Function {
  public CppFunction(Identifier identifier,
                     Parameter[] parameters,
                     Type returnType,
                     Graph behavior) {
    super(identifier, parameters, returnType, behavior);
  }

  public CppFunction(Function function) {
    super(function.identifier, function.parameters(), function.returnType(),
        function.behavior());
  }

  public CppFunctionName functionName() {
    return new CppFunctionName(identifier);
  }
}
