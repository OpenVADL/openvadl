package vadl.oop.passes.type_normalization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts bit mask to ensure that the code generation works.
 */
public abstract class CppTypeNormalizationPass extends Pass {

  /**
   * Get a list of functions on which the pass should be applied on.
   */
  protected abstract Stream<Function> getApplicable(Specification viam);

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    getApplicable(viam).forEach(function -> {
      var typeNormalizer = new CppTypeNormalizer();
      typeNormalizer.makeTypesCppConform(function);
    });

    return null;
  }
}
