package vadl.iss.template.hw;

import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.common.PureFunctionCodeGenerator;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.annotations.EnableHtifAnno;

/**
 * Emits the {@code hw/gen-arch/virt.c} which is the core implementation for the
 * virtual hardware board.
 * It defines things like memory, start address, HTIF, firmware loading, etc...
 */
public class EmitIssVirtCPass extends IssTemplateRenderingPass {

  public EmitIssVirtCPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "hw/gen-arch/virt.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    vars.put("dram_base", getDramBaseExpr());
    vars.put("start_addr", getStartAddrExpr(specification));
    vars.put("htif_enabled", htifEnabled(specification));
    return vars;
  }


  private String getDramBaseExpr() {
    // TODO: Don't hardcode this
    return "0x80000000";
  }

  private String getStartAddrExpr(Specification specification) {
    var mip = specification.mip().orElse(null);
    specification.ensure(mip != null, "No MicroProcessor definition found");
    return new PureFunctionCodeGenerator(mip.start()).genReturnExpression();
  }

  private boolean htifEnabled(Specification specification) {
    var mip = specification.mip().orElse(null);
    specification.ensure(mip != null, "No MicroProcessor definition found");
    return mip.hasAnnotation(EnableHtifAnno.class);
  }
}
