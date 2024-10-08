package vadl.iss.template.target;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/insn.decode that contains the decoding tree for the
 * given ISA.
 * This file is used during ISS compile time to generate a C decoder that calls
 * the correct translation functions for given instructions.
 */
public class EmitIssInsnDecodePass extends IssTemplateRenderingPass {
  public EmitIssInsnDecodePass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/insn.decode";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    return super.createVariables(passResults, specification);
  }
}
