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

  /**
   * Converts a given {@link Function} into a {@link CppFunction}.
   */
  public CppFunction(Function function) {
    super(function.identifier,
        function.parameters(),
        function.returnType(),
        function.behavior());
  }

  /**
   * Converts a given {@link Function} into a {@link CppFunction} and extends the identifier with
   * the given {@code suffix}.
   */
  public CppFunction(Function function, String suffix) {
    super(function.identifier.append(suffix),
        function.parameters(),
        function.returnType(),
        function.behavior());
  }

  public CppFunctionName functionName() {
    return new CppFunctionName(identifier);
  }
}
