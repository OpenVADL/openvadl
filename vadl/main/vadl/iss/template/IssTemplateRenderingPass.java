package vadl.iss.template;

import java.util.HashMap;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

public abstract class IssTemplateRenderingPass extends AbstractTemplateRenderingPass {

  public IssTemplateRenderingPass(IssConfiguration configuration) {
    super(configuration, "iss");
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }

  @Override
  protected final String getTemplatePath() {
    return "iss/" + issTemplatePath();
  }

  protected abstract String issTemplatePath();

  @Override
  protected String getOutputPath() {
    var templatePath = issTemplatePath();

    // the iss template path is in the same hierarchy as the generated files.
    // however, if the path includes the generated architecture name, the template paths
    // use `gen-arch`, which must be replaced by the actual architecture name.
    return templatePath
        .replaceAll("gen-arch", configuration().architectureName());
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = new HashMap<String, Object>();
    vars.put("gen_arch", configuration().architectureName().toLowerCase());
    vars.put("gen_arch_upper", configuration().architectureName().toUpperCase());
    return vars;
  }
}
