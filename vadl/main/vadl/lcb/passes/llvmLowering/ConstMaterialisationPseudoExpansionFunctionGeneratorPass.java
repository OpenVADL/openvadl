package vadl.lcb.passes.llvmLowering;

import java.util.List;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.gcb.passes.pseudo.AbstractPseudoExpansionFunctionGeneratorPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

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
  protected Stream<PseudoInstruction> getApplicable(PassResults passResults,
                                                    Specification viam) {
    return ((List<PseudoInstruction>) passResults.lastResultOf(
        GenerateConstantMaterialisationPass.class)).stream();
  }

  @Override
  public PassName getName() {
    return new PassName("ConstMaterialisationPseudoExpansionFunctionGeneratorPass");
  }
}
