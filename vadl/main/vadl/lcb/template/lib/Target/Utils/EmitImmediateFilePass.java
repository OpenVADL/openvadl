package vadl.lcb.template.lib.Target.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.lcb.codegen.encoding.DecodingCodeGenerator;
import vadl.lcb.codegen.encoding.EncodingCodeGenerator;
import vadl.lcb.codegen.encoding.PredicateCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.superClass.AbstractEmitImmediateFilePass;
import vadl.pass.PassResults;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * This file contains all the immediates for TableGen.
 */
public class EmitImmediateFilePass extends AbstractEmitImmediateFilePass {

  public EmitImmediateFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Utils/ImmediateUtils.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "lcb/llvm/lib/Target/" + processorName + "/Utils/"
        + processorName + "ImmediateUtils.h";
  }
}
