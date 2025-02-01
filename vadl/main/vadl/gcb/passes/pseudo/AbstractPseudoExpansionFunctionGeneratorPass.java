package vadl.gcb.passes.pseudo;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppGenericType;
import vadl.cppCodeGen.model.CppParameter;
import vadl.cppCodeGen.model.CppType;
import vadl.pass.Pass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * The {@link PseudoExpansionCodeGenerator} requires a function to generate the expansion.
 * However, we only have a {@link Graph} as behavior. This pass wraps the graph to a
 * {@link Function}.
 */
public abstract class AbstractPseudoExpansionFunctionGeneratorPass extends Pass {
  public AbstractPseudoExpansionFunctionGeneratorPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  /**
   * Get the instructions for which {@link CppFunction} should be generated.
   */
  protected abstract Stream<Pair<PseudoInstruction, Graph>> getApplicable(
      PassResults passResults,
      Specification viam);

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new IdentityHashMap<PseudoInstruction, CppFunction>();

    getApplicable(passResults, viam)
        .forEach(x -> {
          var pseudoInstruction = x.left();
          var ty = new CppType("MCInst", true, true);
          var param = new CppParameter(new Identifier("instruction",
              SourceLocation.INVALID_SOURCE_LOCATION),
              ty);
          var function = new CppFunction(pseudoInstruction.identifier.append("expand"),
              new Parameter[] {param},
              new CppGenericType("std::vector", new CppType("MCInst", false, false)),
              pseudoInstruction.behavior());

          result.put(pseudoInstruction, function);
        });

    return result;
  }
}
