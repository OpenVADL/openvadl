package vadl.gcb.passes.typeNormalization;

import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.pass.PassName;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts a bit mask to ensure that the code generation works for predicates.
 */
public class CppTypeNormalizationForPredicatesPass extends CppTypeNormalizationPass {
  public CppTypeNormalizationForPredicatesPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName(CppTypeNormalizationForPredicatesPass.class.getName());
  }

  @Override
  protected Stream<Pair<Format.Field, Function>> getApplicable(Specification viam) {
    return viam.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(x -> x.fieldAccesses().stream())
        .filter(x -> x.encoding() != null)
        .map(fieldAccess -> new Pair<>(fieldAccess.fieldRef(), fieldAccess.predicate()));
  }

  @Override
  protected CppFunction liftFunction(Function function) {
    return makeTypesCppConform(function);
  }
}
