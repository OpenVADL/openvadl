package vadl.gcb.passes.type_normalization;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.pass.PassName;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts a bit mask to ensure that the code generation works for decodings.
 */
public class CppTypeNormalizationForDecodingsPass extends CppTypeNormalizationPass {

  public CppTypeNormalizationForDecodingsPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName(CppTypeNormalizationForDecodingsPass.class.getName());
  }

  @Override
  protected Stream<Pair<Format.Field, Function>> getApplicable(Specification viam) {
    return viam.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(x -> new Pair<>(x.fieldRef(), x.accessFunction()));
  }

  @Override
  protected CppFunction liftFunction(Function function) {
    // LLVM's decoder requires uint64_t parameters.
    return makeTypesCppConformWithParamType(function, getParameters(function));
  }

  private static List<Parameter> getParameters(Function function) {
    return Arrays.stream(function.parameters())
        .map(CppTypeNormalizationForDecodingsPass::upcast)
        .toList();
  }

  private static Parameter upcast(Parameter parameter) {
    return new Parameter(parameter.identifier,
        upcast(parameter.type()), parameter.parent());
  }

  private static BitsType upcast(Type type) {
    if (type instanceof BitsType cast) {
      return cast.withBitWidth(64);
    } else {
      throw new ViamError("Non bits type are not supported");
    }
  }
}
