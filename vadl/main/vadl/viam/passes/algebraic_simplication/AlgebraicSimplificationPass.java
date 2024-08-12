package vadl.viam.passes.algebraic_simplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AdditionWithZeroSimplificationRule;

/**
 * It looks at all the {@link BuiltInCall} nodes and tries to match a static set of rules.
 * If a rule matches then the {@link BuiltInCall} will be simplified.
 * It will repeat the process until nothing changes.
 * It will only consider machine instructions.
 */
public class AlgebraicSimplificationPass extends Pass {
  private static final List<AlgebraicSimplificationRule> rules = new ArrayList<>();

  static {
    rules.add(new AdditionWithZeroSimplificationRule());
  }

  @Override
  public PassName getName() {
    return new PassName("constantFolding");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam) {
    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> new AlgebraicSimplifier(rules).run(instruction.behavior()));

    viam.isas()
        .flatMap(isa -> isa.formats().stream())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(x -> x.accessFunction().behavior())
        .forEach(x -> new AlgebraicSimplifier(rules).run(x));

    return null;
  }
}
