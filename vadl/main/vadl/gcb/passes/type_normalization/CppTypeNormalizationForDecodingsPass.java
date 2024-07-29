package vadl.gcb.passes.type_normalization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.oop.passes.type_normalization.CppTypeNormalizationPass;
import vadl.oop.passes.type_normalization.CppTypeNormalizer;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts bit mask to ensure that the code generation works for decodings.
 */
public class CppTypeNormalizationForDecodingsPass extends CppTypeNormalizationPass {

  @Override
  public PassName getName() {
    return new PassName("CppTypeNormalizationForDecodings");
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(Format.FieldAccess::accessFunction)
        .filter(Objects::nonNull);
  }
}
