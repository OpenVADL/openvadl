package vadl.lcb.passes.llvmLowering.compensation;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;

import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.llvmLowering.compensation.strategies.LlvmCompensationPatternStrategy;
import vadl.lcb.passes.llvmLowering.compensation.strategies.LlvmCompensationRotateLeftPatternStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Not every ISA has all the required instructions. For example, RISC-V has no machine instruction
 * for rotate-left. This pass will detect missing patterns and generate pattern, so they are covered
 * during instruction selection in LLVM.
 */
public class CompensationPatternPass extends Pass {
  private final List<LlvmCompensationPatternStrategy> patternStrategies =
      List.of(new LlvmCompensationRotateLeftPatternStrategy());

  public CompensationPatternPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("CompensationPatternPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var patterns = new ArrayList<TableGenPattern>();
    var database = new Database(passResults, viam);

    for (var strategy : patternStrategies) {
      if (strategy.isApplicable(database)) {
        patterns.addAll(strategy.lower(database, viam));
      }
    }

    return patterns;
  }
}
