package vadl.iss.template.target;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/translate.c that contains the functions to generate
 * the TCG instructions from decoded guest instructions.
 * It also contains the {@code gen_intermediate_code} function, called by QEMU as
 * entry point to start the TCG generation.
 */
public class EmitIssTranslatePass extends IssTemplateRenderingPass {
  public EmitIssTranslatePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/translate.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return super.createVariables(passResults, specification);
  }
}
