package vadl.gcb.passes.type_normalization;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts a bit mask to ensure that the code generation works for encodings.
 */
public class CppTypeNormalizationForEncodingsPass extends CppTypeNormalizationPass {

  public CppTypeNormalizationForEncodingsPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName(CppTypeNormalizationForEncodingsPass.class.getName());
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isas()
        .flatMap(x -> x.ownFormats().stream())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(Format.FieldAccess::encoding)
        .filter(Objects::nonNull);
  }
}
