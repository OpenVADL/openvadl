package vadl.iss.template.target;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Register;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/cpu.c file that contains all required
 * CPU function/method implementations required by QEMU.
 */
public class EmitIssCpuSourcePass extends IssTemplateRenderingPass {
  public EmitIssCpuSourcePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/cpu.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);

    return vars;
  }
}
