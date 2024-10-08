package vadl.iss.template.target;

import static vadl.iss.template.IssRenderUtils.mapPc;
import static vadl.iss.template.IssRenderUtils.mapRegFiles;
import static vadl.iss.template.IssRenderUtils.mapRegs;

import java.util.HashMap;
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
 * Emits the target/gen-arch/cpu.h file that contains all required
 * CPU definitions required by QEMU.
 */
public class EmitIssCpuHeaderPass extends IssTemplateRenderingPass {
  public EmitIssCpuHeaderPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/cpu.h";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("pc_reg_name", mapPc(specification).get("name_lower"));
    vars.put("pc_reg_c_type", mapPc(specification).get("c_type"));
    return vars;
  }

}
