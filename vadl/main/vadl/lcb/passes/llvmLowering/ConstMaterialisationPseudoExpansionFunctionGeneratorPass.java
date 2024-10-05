package vadl.lcb.passes.llvmLowering;

import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.gcb.passes.pseudo.AbstractPseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.pseudo.AbstractPseudoInstructionArgumentReplacementPass;
import vadl.gcb.passes.pseudo.ConstMatPseudoInstructionArgumentReplacementPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * Expand {@link ConstantMatPseudoInstruction} to {@link CppFunction} for expansion.
 */
public class ConstMaterialisationPseudoExpansionFunctionGeneratorPass extends
    AbstractPseudoExpansionFunctionGeneratorPass {
  public ConstMaterialisationPseudoExpansionFunctionGeneratorPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected Stream<Pair<PseudoInstruction, Graph>> getApplicable(PassResults passResults,
                                                                 Specification viam) {
    var appliedArguments =
        (AbstractPseudoInstructionArgumentReplacementPass.Output) passResults.lastResultOf(
            ConstMatPseudoInstructionArgumentReplacementPass.class);
    return appliedArguments.appliedGraph().entrySet().stream()
        .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
  }

  @Override
  public PassName getName() {
    return new PassName("ConstMaterialisationPseudoExpansionFunctionGeneratorPass");
  }
}
