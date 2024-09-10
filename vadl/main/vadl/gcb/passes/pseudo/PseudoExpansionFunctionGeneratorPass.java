package vadl.gcb.passes.pseudo;

import java.io.IOException;
import java.util.IdentityHashMap;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.CppGenericType;
import vadl.cppCodeGen.model.CppParameter;
import vadl.cppCodeGen.model.CppType;
import vadl.lcb.codegen.expansion.PseudoExpansionCodeGenerator;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
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
public class PseudoExpansionFunctionGeneratorPass extends Pass {
  protected PseudoExpansionFunctionGeneratorPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("pseudoExpansionFunctionGeneratorPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new IdentityHashMap<PseudoInstruction, Function>();

    viam.isas()
        .flatMap(isa -> isa.ownPseudoInstructions().stream())
        .forEach(pseudoInstruction -> {
          var ty = new CppType("MCInst", true, true);
          var param = new CppParameter(new Identifier("instruction",
              SourceLocation.INVALID_SOURCE_LOCATION),
              ty);
          var function = new Function(pseudoInstruction.identifier.append("expand"),
              new Parameter[] {param}, new CppGenericType("std::vector", "MCInst"),
              pseudoInstruction.behavior());

          result.put(pseudoInstruction, function);
        });

    return result;
  }
}
