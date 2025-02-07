package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.Map;
import vadl.ast.AsmDirective;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmParserCommentString;
import vadl.viam.asm.AsmDirectiveMapping;

/**
 * This file contains the implementation for general assembly info.
 */
public class EmitMCAsmInfoCppFilePass extends LcbTemplateRenderingPass {

  public EmitMCAsmInfoCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCAsmInfo.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCAsmInfo.cpp";
  }

  record AssemblyDescription(String commentString, boolean alignmentInBytes) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "commentString", commentString,
          "alignmentInBytes", alignmentInBytes
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        CommonVarNames.ASSEMBLY_DESCRIPTION,
        new AssemblyDescription(asmCommentString(specification),
            asmAlignmentIsInBytes(specification))
    );
  }

  private String asmCommentString(Specification specification) {
    return specification.assemblyDescription()
        .map(asmDesc -> asmDesc.annotation(AsmParserCommentString.class))
        .map(AsmParserCommentString::getCommentString).orElse("#");
  }

  private boolean asmAlignmentIsInBytes(Specification specification) {

    return specification.assemblyDescription()
        .map(asmDesc -> asmDesc.directives().stream()
            .noneMatch(AsmDirectiveMapping::getAlignmentIsInBytes)
        ).orElse(true);
  }
}
