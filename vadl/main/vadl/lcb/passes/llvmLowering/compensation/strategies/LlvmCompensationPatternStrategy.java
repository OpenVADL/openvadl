package vadl.lcb.passes.llvmLowering.compensation.strategies;

import java.util.Collection;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Specification;

/**
 * Defines a strategy how to generate a compensation pattern.
 */
public interface LlvmCompensationPatternStrategy {
  /**
   * Checks whether the strategy has to be applied.
   * It runs the returned query on the {@link Database} and if no result exists then
   * the strategy is applicable.
   */
  boolean isApplicable(Database database);

  /**
   * Generates a pattern with this strategy.
   */
  Collection<? extends TableGenPattern> lower(Database database, Specification viam);
}
