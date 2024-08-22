package vadl.pass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Specification;

/**
 * The {@link PassManager} manages the execution of multiple {@link Pass}.
 * Note that it is possible to schedule the same {@link Pass} multiple times.
 * The execution of the passes happens in the same order as they were inserted.
 */
public class PassManager {
  private static final Logger logger = LoggerFactory.getLogger(PassManager.class);
  /**
   * Stores the results of the passes.
   */
  private final PassResults passResults = new PassResults();
  private final List<PassStep> pipeline = new ArrayList<>();

  private boolean hasDuplicatedPassKey(PassKey needle) {
    var keys = pipeline.stream().map(PassStep::key).collect(Collectors.toSet());
    return keys.contains(needle);
  }

  /**
   * Add a new passes to the pipeline.
   * The results are available with {@code pass}'s class as {@link PassKey}.
   *
   * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName}
   *                                    was added.
   */
  public void add(List<Pass> passes) throws DuplicatedPassKeyException {
    for (var pass : passes) {
      add(pass);
    }
  }

  /**
   * Add a new pass to the pipeline.
   * The results are available with {@code pass}'s class as {@link PassKey}.
   *
   * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName}
   *                                    was added.
   */
  public void add(Pass pass) throws DuplicatedPassKeyException {
    add(new PassKey(pass.getClass().getName()), pass);
  }

  /**
   * Add a new pass to the pipeline.
   *
   * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName}
   *                                    was added.
   */
  public void add(PassKey key, Pass pass) throws DuplicatedPassKeyException {
    logger.atDebug().log("Adding pass with key: {}", key.value());
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
      logger.atDebug().log("Running pass with key: {}", step.key());
      // Wrapping the passResults into an unmodifiable map so a pass cannot modify
      // the results.
      var passResult = step.pass().execute(passResults, viam);

      if (passResult != null) {
        logger.atDebug().log("Storing result of pass with key: {}", step.key());
        passResults.add(step.key(), passResult);
      }
      logger.atDebug().log("Pass completed with key: {}", step.key());
    }
  }

  public PassResults getPassResults() {
    return this.passResults;
  }
}
