package vadl.iss.template.target.decode.qemu;

import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.common.AccessFunctionCodeGenerator;
import vadl.iss.passes.decode.qemu.QemuDecodeSymbolResolvingPass;
import vadl.iss.passes.decode.qemu.dto.Field;
import vadl.iss.passes.decode.qemu.dto.QemuDecodeResolveSymbolPassResult;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Specification;

/**
 * Emits the target/gen-arch/insn-access.h that contains the function signatures for the
 * access functions required by the QEMU decode tree.
 */
public class EmitIssInsnAccessHeaderPass extends IssTemplateRenderingPass {

  public static final String ACCESS_SGN_KEY = "insn_access_signatures";

  public EmitIssInsnAccessHeaderPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/insn-access.h";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {

    final Map<String, Object> variables = super.createVariables(passResults, specification);

    if (!passResults.hasRunPassOnce(QemuDecodeSymbolResolvingPass.class)) {
      // Nothing to enrich
      return variables;
    }

    final var qemuDefs = passResults.lastResultOf(QemuDecodeSymbolResolvingPass.class,
        QemuDecodeResolveSymbolPassResult.class);

    final List<Field> pseudoFields = qemuDefs.fields().stream()
        .filter(f -> f.getSource() instanceof Format.FieldAccess)
        .toList();

    if (pseudoFields.isEmpty()) {
      // Nothing to do, no field accesses
      return variables;
    }

    final List<String> fnProtos = pseudoFields.stream()
        .distinct()
        .map(f -> new AccessFunctionCodeGenerator((Format.FieldAccess) f.getSource(),
            f.getDecodeFunction()))
        .map(AccessFunctionCodeGenerator::genFunctionSignature)
        .map(String::strip).map(s -> s + ";")
        .toList();

    variables.put(ACCESS_SGN_KEY, fnProtos);
    return variables;
  }
}
