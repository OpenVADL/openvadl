package vadl.gcb.passes.typeNormalization;

import java.util.Arrays;
import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.pass.PassName;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts a bit mask to ensure that the code generation works for immediate
 * extraction.
 */
public class CppTypeNormalizationForImmediateExtractionPass extends CppTypeNormalizationPass {

  public CppTypeNormalizationForImmediateExtractionPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName(CppTypeNormalizationForImmediateExtractionPass.class.getName());
  }

  @Override
  protected Stream<Pair<Format.FieldAccess, Function>> getApplicable(Specification viam) {
    return viam.isa()
        .map(x -> x.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(x -> x.fieldAccesses().stream())
        .map(fieldAccess -> new Pair<>(fieldAccess, fieldAccess.fieldRef().extractFunction()))
        .distinct();
  }

  @Override
  protected GcbFieldAccessCppFunction liftFunction(Format.FieldAccess fieldAccess) {
    return createGcbFieldAccessCppFunction(fieldAccess.accessFunction(), fieldAccess);
  }
}
