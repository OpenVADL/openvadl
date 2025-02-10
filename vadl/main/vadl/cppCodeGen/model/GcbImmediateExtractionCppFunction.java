package vadl.cppCodeGen.model;

import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;

/**
 * An extension of a {@link Function} which has more information about the generated code.
 * This class symbols the given function will extract an immediate from the {@link Instruction}.
 */
public class GcbImmediateExtractionCppFunction extends Function {
  public GcbImmediateExtractionCppFunction(Identifier identifier,
                                           Parameter[] parameters,
                                           Type returnType,
                                           Graph behavior) {
    super(identifier, parameters, returnType, behavior);
  }

  /**
   * The {@link vadl.viam.Format.Field#extractFunction()} returns a {@link Function}.
   * However, when working with relocations, we want to extract the immediate. Therefore, we need
   * to convert {@link GcbFieldAccessCppFunction} into a {@link GcbImmediateExtractionCppFunction}.
   */
  public GcbImmediateExtractionCppFunction(Function extractionFunction) {
    super(extractionFunction.identifier,
        extractionFunction.parameters(),
        extractionFunction.returnType(),
        extractionFunction.behavior());
  }

  public CppFunctionName functionName() {
    return new CppFunctionName(identifier);
  }
}
