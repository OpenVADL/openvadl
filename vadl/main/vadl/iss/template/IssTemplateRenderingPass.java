package vadl.iss.template;

import static vadl.iss.template.IssRenderUtils.mapRegFiles;
import static vadl.iss.template.IssRenderUtils.mapRegs;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.formatting.ClangFormatter;
import vadl.cppCodeGen.formatting.CodeFormatter;
import vadl.iss.codegen.QemuClangFormatter;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * The template rendering pass all ISS (QEMU) rendering passes extend from.
 *
 * <p>It overrides the {@link #getOutputPath()} and uses the {@link #issTemplatePath()}
 * to determine the output location.
 * This is done by replacing all occurrences of {@code gen-arch} in the path, by the
 * {@link IssConfiguration#architectureName()}.
 *
 * <p>All subclasses must provide the {@link #issTemplatePath()} that defines the
 * path inside the {@code resource/templates/iss} directory.
 *
 * <p>It also sets some default variables, required by many templates.
 * Subclasses should use the map returned by {@code super.createVariables} when overriding
 * the {@link #createVariables(PassResults, Specification)} method.
 */
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

  /**
   * The path to the template within th {@code resource/templates/iss} directory.
   */
  protected abstract String issTemplatePath();

  @Override
  public PassName getName() {
    return PassName.of("Rendering ISS " + issTemplatePath());
  }

  @Override
  public @Nullable CodeFormatter getFormatter() {
    if (issTemplatePath().endsWith("translate.c")) {
      return QemuClangFormatter.INSTANCE;

    }
    return null;
  }

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
    vars.put("gen_arch_lower", configuration().architectureName().toLowerCase());
    vars.put("register_files", mapRegFiles(specification));
    vars.put("registers", mapRegs(specification));
    vars.put("insn_count", configuration().isInsnCounting());
    vars.put("target_size", configuration().targetSize().width);
    return vars;
  }
}
