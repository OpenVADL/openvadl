package vadl.pass;

import java.util.Map;
import vadl.viam.Specification;

/**
 * A pass is a unit of execution. It analysis or transforms a VADL specification.
 */
public interface Pass {
  /**
   * Get the name of the pass.
   */
  PassName getName();

  /**
   * Execute the pass on the {@link Specification}.
   *
   * @param passResults are the results from the different passes which have been executed so far.
   * @param viam        is latest VADL specification. Note that transformation passes are allowed
   *                    to mutate the object.
   * @return the result of the pass. This will be automatically stored into {@code passResults} for
   *     the next pass by the {@link PassManager}.
   */
  Object execute(final Map<PassKey, Object> passResults, Specification viam);
}
