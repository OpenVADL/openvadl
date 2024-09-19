package vadl.iss.template.target;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public class EmitIssCpuQomHeaderPass extends IssTemplateRenderingPass {
  public EmitIssCpuQomHeaderPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/cpu-qom.h";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return super.createVariables(passResults, specification);
  }
}
