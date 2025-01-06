package vadl.viam.passes.typeCastElimination;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;

/**
 * Runs the type cast elimination on ALL behaviors in the given VIAM specification.
 *
 * @see TypeCastEliminator
 */
public class TypeCastEliminationPass extends Pass {
  public TypeCastEliminationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("typeCastElimination");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    ViamUtils.findDefinitionsByFilter(viam, DefProp.WithBehavior.class::isInstance)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .flatMap(definition -> definition.behaviors().stream())
        .forEach(TypeCastEliminator::runOnGraph);

    return null;
  }
}
