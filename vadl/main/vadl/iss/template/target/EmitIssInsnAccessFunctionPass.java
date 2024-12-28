package vadl.iss.template.target;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.common.AccessFunctionCodeGenerator;
import vadl.iss.passes.decode.QemuDecodeSymbolResolvingPass;
import vadl.iss.passes.decode.dto.QemuDecodeResolveSymbolPassResult;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Specification;

public class EmitIssInsnAccessFunctionPass extends IssTemplateRenderingPass {

  public static final String ACCESS_FNS_KEY = "insn_access";

  public EmitIssInsnAccessFunctionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/insn-access.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {

    final Map<String, Object> variables = super.createVariables(passResults, specification);

    final var passResult = passResults.lastResultOf(QemuDecodeSymbolResolvingPass.class);
    if (!(passResult instanceof QemuDecodeResolveSymbolPassResult qemuDefs)) {
      // Nothing to enrich
      return variables;
    }

    final List<Format.FieldAccess> fieldAccesses = qemuDefs.fields().stream()
        .filter(f -> f.getSource() instanceof Format.FieldAccess)
        .map(f -> (Format.FieldAccess) f.getSource())
        .toList();

    if (fieldAccesses.isEmpty()) {
      // Nothing to do, no field accesses
      return variables;
    }

    // TODO: handle function name collision
    final List<String> fnDefs = fieldAccesses.stream()
        .distinct()
        .map(a -> new AccessFunctionCodeGenerator(a, a.accessFunction().simpleName()))
        .map(AccessFunctionCodeGenerator::genFunctionDefinition)
        .toList();

    variables.put(ACCESS_FNS_KEY, fnDefs);

    return variables;
  }
}
