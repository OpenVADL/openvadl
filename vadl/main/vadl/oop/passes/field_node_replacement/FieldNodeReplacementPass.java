package vadl.oop.passes.field_node_replacement;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * We cannot access fields when generating cpp functions. Field accesses
 * will be replaced to {@link FuncParamNode}.
 */
public abstract class FieldNodeReplacementPass extends Pass {
 
  /**
   * Get a list of functions on which the pass should be applied on.
   */
  protected abstract Stream<Function> getApplicable(Specification viam);

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {

    getApplicable(viam).forEach(function -> {

    });

    return null;
  }

}
