package vadl.gcb.passes.type_normalization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.oop.passes.type_normalization.CppTypeNormalizer;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts bit mask to ensure that the code generation works for decodings.
 */
public class CppTypeNormalizationForDecodingsPass extends Pass {

  @Override
  public PassName getName() {
    return new PassName("CppTypeNormalizationForDecodings");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() != null)
        .forEach(fieldAccess -> {
          var typeNormalizer = new CppTypeNormalizer();
          typeNormalizer.makeTypesCppConform(fieldAccess.accessFunction());
        });

    return null;
  }
}
