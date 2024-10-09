package vadl.gcb.passes.type_normalization;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.Arrays;
import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.error.Diagnostic;
import vadl.pass.PassName;
import vadl.utils.Pair;
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
  protected Stream<Pair<Format.Field, Function>> getApplicable(Specification viam) {
    return viam.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(fieldAccess -> new Pair<>(fieldAccess.fieldRef(),
            ensureNonNull(fieldAccess.encoding(),
                () -> Diagnostic.error(
                    "Encoding must not be null. Maybe it does not exist or was not generated?",
                    fieldAccess.sourceLocation())
            )));
  }

  @Override
  protected CppFunction liftFunction(Function function) {
    return makeTypesCppConform(function);
  }
}
