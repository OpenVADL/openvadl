package vadl.iss.template.target;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

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
    vars.put("register_files", getRegisterFiles(specification));
    return vars;
  }

  private static List<Map<String, String>> getRegisterFiles(Specification specification) {
    return specification.isa().get()
        .ownRegisterFiles()
        .stream()
        .map(rf -> Map.of(
            "name", rf.identifier.simpleName()
            , "name_upper", rf.identifier.simpleName().toUpperCase()
            , "name_lower", rf.identifier.simpleName().toLowerCase()
            , "size", String.valueOf((int) Math.pow(2, rf.addressType().bitWidth()))
            , "value_c_type", CppTypeMap.getCppTypeNameByVadlType(rf.resultType())
        ))
        .toList();
  }
}
