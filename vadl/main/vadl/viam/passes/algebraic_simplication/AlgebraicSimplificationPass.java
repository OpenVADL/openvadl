package vadl.viam.passes.algebraic_simplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AdditionWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AndWithFalseSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AndWithTrueSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.DivisionWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.MultiplicationWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.MultiplicationWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.OrWithFalseSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.OrWithTrueSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.RemainderWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.RemainderWithZeroSimplificationRule;

/**
 * It looks at all the {@link BuiltInCall} nodes and tries to match a static set of rules.
 * If a rule matches then the {@link BuiltInCall} will be simplified.
 * It will repeat the process until nothing changes.
 * It will only consider machine instructions.
 */
public class AlgebraicSimplificationPass extends Pass {
  public static final List<AlgebraicSimplificationRule> rules = new ArrayList<>();

  static {
    rules.add(new AdditionWithZeroSimplificationRule());
    rules.add(new MultiplicationWithZeroSimplificationRule());
    rules.add(new MultiplicationWithOneSimplificationRule());
    rules.add(new DivisionWithOneSimplificationRule());
    rules.add(new RemainderWithZeroSimplificationRule());
    rules.add(new RemainderWithOneSimplificationRule());
    rules.add(new AndWithFalseSimplificationRule());
    rules.add(new AndWithTrueSimplificationRule());
    rules.add(new OrWithTrueSimplificationRule());
    rules.add(new OrWithFalseSimplificationRule());
  }

  public AlgebraicSimplificationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("constantFolding");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> new AlgebraicSimplifier(rules).run(instruction.behavior()));

    viam.isa().map(isa -> isa.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(x -> x.accessFunction().behavior())
        .forEach(x -> new AlgebraicSimplifier(rules).run(x));

    return null;
  }
}
