package vadl.viam.passes.behaviorRewrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.behaviorRewrite.rules.BehaviorRewriteSimplificationRule;
import vadl.viam.passes.behaviorRewrite.rules.impl.MergeSMullAndTruncateToMulSimplificationRule;

/**
 * This pass should provide more generic rewrites than {@link AlgebraicSimplificationPass}.
 */
public class BehaviorRewritePass extends Pass {
  public static final List<BehaviorRewriteSimplificationRule> rules = new ArrayList<>();

  static {
    rules.add(new MergeSMullAndTruncateToMulSimplificationRule());
  }

  public BehaviorRewritePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("behaviorRewritePass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> new BehaviorRewriteSimplifier(rules).run(instruction.behavior()));

    return null;
  }
}
