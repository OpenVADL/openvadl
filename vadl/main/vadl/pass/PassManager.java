package vadl.pass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.dump.CollectBehaviorDotGraphPass;
import vadl.dump.HtmlDumpPass;
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
   * Add all passes/steps in the {@link PassOrder} to the pipeline.
   *
   * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName}
   *                                    was added.
   */
  public void add(PassOrder passOrder) throws DuplicatedPassKeyException {
    for (var step : passOrder.passSteps()) {
      add(step);
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
    add(new PassStep(key, pass));
  }

  /**
   * Add a new pass to the pipeline.
   *
   * @throws DuplicatedPassKeyException when pass with an already existing {@link PassName}
   *                                    was added.
   */
  public void add(PassStep passStep) throws DuplicatedPassKeyException {
    logger.debug("Adding pass with key: {}", passStep.key().value());
    if (hasDuplicatedPassKey(passStep.key())) {
      throw new DuplicatedPassKeyException(passStep.key());
    }

    this.pipeline.add(passStep);
  }

  /**
   * Run all the passes in the order which they have been added.
   */
  public void run(Specification viam) throws IOException {
    // Accept every pass to run the whole pipeline.
    run(viam, passKey -> false);
  }

  /**
   * Run all the passes in the order which they have been added when the
   * {@link java.util.function.Predicate} matches.
   */
  // TODO: Remove or rework predicate argument
  private void run(Specification viam, Predicate<PassKey> predicate) throws IOException {
    var affectedSteps = new ArrayList<PassStep>();

    // Go over pipeline and check whether the predicate matches.
    // If it does then stop adding passes to `affectedSteps`.
    for (var step : pipeline) {
      affectedSteps.add(step);
      if (predicate.test(step.key())) {
        break;
      }
    }

    for (var step : affectedSteps) {
      @SuppressWarnings("VariableDeclarationUsageDistance")
      var startTime = System.currentTimeMillis();
      logger.debug("Running pass with key: {}", step.key());
      // Wrapping the passResults into an unmodifiable map so a pass cannot modify
      // the results.
      var pass = step.pass();
      var passResult = execPass(pass, viam);
      pass.verification(viam, passResult);

      // we always store the pass result, even if the result is `null`
      logger.debug("Storing result of pass with key: {}", step.key());
      var duration = System.currentTimeMillis() - startTime;
      passResults.add(step.key(), pass, duration, passResult);

      logger.debug("Pass completed: {} -- {} ms", step.key(),
          duration);
    }
  }


  // executes the pass and dumps the VIAM if an exception occurs.
  private @Nullable Object execPass(Pass pass, Specification viam) throws IOException {
    try {
      return pass.execute(passResults, viam);
    } catch (Exception e) {
      var config = pipeline.get(0).pass().configuration();
      var passClassName = pass.getClass().getSimpleName();
      // collect latest graphs
      var graphCollectPass = new CollectBehaviorDotGraphPass(pass.configuration());
      var graphCollectResult = graphCollectPass.execute(passResults, viam);
      passResults.add(PassKey.of("BehaviorCollectionOnException"), graphCollectPass, 0,
          graphCollectResult);
      // on an exception, we do an emergency dump
      var htmlDumpPass = new HtmlDumpPass(HtmlDumpPass.Config
          .from(config, "Exception During " + passClassName,
              "This is a dump after exception occurred during the %s pass."
                  .formatted(passClassName)));
      htmlDumpPass.execute(passResults, viam);
      throw e;
    }
  }

  /**
   * Run all the passes in the order which they have been added until the {@link Pass}
   * with the given {@code passKey} (inclusive).
   */
  // TODO: Remove
  @Deprecated
  public void runUntilInclusive(Specification spec, PassKey passKey) throws IOException {
    run(spec, passKey::equals);
  }

  public PassResults getPassResults() {
    return this.passResults;
  }
}
