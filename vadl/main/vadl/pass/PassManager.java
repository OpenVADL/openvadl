package vadl.pass;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Specification;

/**
 * The {@link PassManager} manages the execution of multiple {@link Pass}.
 * Note that it is possible to schedule the same {@link Pass} multiple times.
 * The execution of the passes happens in the same order as they were inserted.
 */
public class PassManager {
  /**
   * Stores the results of the passes.
   */
  private final HashMap<PassKey, Object> passResults = new HashMap<>();
  private final List<PassStep> pipeline = new ArrayList<>();

  private boolean hasDuplicatedPassKey(PassKey needle) {
    var keys = pipeline.stream().map(PassStep::key).collect(Collectors.toSet());
    return keys.contains(needle);
  }

  /**
   * Add a new pass to the pipeline.
   *
   * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName}
   *                                    was added.
   */
  public void add(PassKey key, Pass pass) throws DuplicatedPassKeyException {
    if (hasDuplicatedPassKey(key)) {
      throw new DuplicatedPassKeyException(key);
    }

    this.pipeline.add(new PassStep(key, pass));
  }

  /**
   * Run the passes which have been added in order.
   */
  public void run(Specification viam) throws IOException {
    for (var step : pipeline) {
      // Wrapping the passResults into an unmodifiable map so a pass cannot modify
      // the results.
      var passResult = step.pass().execute(Collections.unmodifiableMap(passResults), viam);

      if (passResult != null) {
        var previousResult = passResults.put(step.key(), passResult);

        // The pipeline's steps should be deterministic.
        // If we overwrite an already existing result then it is very likely
        // that it is a bug because we schedule the same pass with the same key multiple times.
        assert previousResult == null;
      }
    }
  }

  public Map<PassKey, Object> getPassResults() {
    return this.passResults;
  }
}
