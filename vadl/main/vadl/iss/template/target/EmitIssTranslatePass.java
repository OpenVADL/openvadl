package vadl.iss.template.target;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

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
