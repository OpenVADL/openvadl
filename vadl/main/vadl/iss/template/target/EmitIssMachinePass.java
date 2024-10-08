package vadl.iss.template.target;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/machine.c that contains VM state information to
 * save and restore the state of QEMU for the generated CPU device.
 *
 * <p>Note that this is not yet activated and doesn't work yet.</p>
 */
// TODO: Make this work (look at machine.c TODO comment).
public class EmitIssMachinePass extends IssTemplateRenderingPass {
  public EmitIssMachinePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/machine.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return super.createVariables(passResults, specification);
  }
}
