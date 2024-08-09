package vadl.viam.passes.typeCastElimination;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.utils.ViamUtils;
import vadl.viam.Specification;
import vadl.viam.WithBehavior;

public class TypeCastEliminationPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("typeCastElimination");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {

    ViamUtils.findDefinitionByFilter(viam, WithBehavior.class::isInstance)
        .stream()
        .map(WithBehavior.class::cast)
        .flatMap(definition -> definition.behaviors().stream())
        .forEach(TypeCastEliminator::runOnGraph);

    return null;
  }
}
