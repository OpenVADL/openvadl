package vadl.viam.passes.canonicalization;

import java.util.Arrays;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
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
    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> Canonicalizer.canonicalize(instruction.behavior()));

    viam.isas()
        .flatMap(isa -> isa.formats().stream())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(x -> x.accessFunction().behavior())
        .forEach(Canonicalizer::canonicalize);

    return null;
  }
}
