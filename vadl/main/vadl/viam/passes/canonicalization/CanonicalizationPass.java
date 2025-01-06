package vadl.viam.passes.canonicalization;

import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * It looks at all the {@link BuiltInCall} nodes and when two inputs are constant
 * then it replaces it with the result. It will repeat the process until nothing changes.
 * It will only consider machine instructions.
 */
public class CanonicalizationPass extends Pass {
  public CanonicalizationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("canonicalization");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {

    ViamUtils.findDefinitionsByFilter(viam,
            definition -> definition instanceof DefProp.WithBehavior)
        .stream()
        .flatMap(d -> ((DefProp.WithBehavior) d).behaviors().stream())
        .forEach(Canonicalizer::canonicalize);

    return null;
  }
}
