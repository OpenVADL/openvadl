package vadl.iss.template.target;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.iss.IssDecisionTreeCodeGenerator;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/vdt-decode.c that contains the decoding tree for the given ISA.
 */
public class EmitIssDecodeTreePass extends IssTemplateRenderingPass {

  private static final String VDT_CODE_KEY = "vdt_code";

  /**
   * Constructor for the pass.
   *
   * @param configuration the ISS configuration
   */
  public EmitIssDecodeTreePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/vdt-decode.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {

    final Map<String, Object> variables = super.createVariables(passResults, specification);

    if (!passResults.hasRunPassOnce(VdtLoweringPass.class)) {
      // Nothing to emit
      return variables;
    }

    final var vdtRoot = passResults.lastResultOf(VdtLoweringPass.class, Node.class);
    final var code = new IssDecisionTreeCodeGenerator().generate(vdtRoot);

    variables.put(VDT_CODE_KEY, code.toString());
    return variables;
  }
}
