package vadl.viam.passes.typeCastElimination;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Specification;
import vadl.viam.WithBehavior;

/**
 * Runs the type cast elimination on ALL behaviors in the given VIAM specification.
 *
 * @see TypeCastEliminator
 */
public class TypeCastEliminationPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("typeCastElimination");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    ViamUtils.findDefinitionByFilter(viam, WithBehavior.class::isInstance)
        .stream()
        .map(WithBehavior.class::cast)
        .flatMap(definition -> definition.behaviors().stream())
        .forEach(TypeCastEliminator::runOnGraph);

    return null;
  }
}
