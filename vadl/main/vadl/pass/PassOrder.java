package vadl.pass;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.HtmlDumpPass;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * This class defines the order in which the {@link PassManager} should run them.
 */
public final class PassOrder {

  // a counter-map that keeps track of how many passes of each pass class exists.
  // this is used to generate a unique pass key if it is not given by the user.
  private static final Map<Class<? extends Pass>, Integer> passCounter
      = new ConcurrentHashMap<>();

  // the actual list of pass steps
  @SuppressWarnings("JdkObsolete")
  private final LinkedList<PassStep> order = new LinkedList<>();

  /**
   * Add a pass to the pass order. If the passKey is null, it will generate a unique one.
   *
   * @return this
   */
  public PassOrder add(@Nullable PassKey passKey, Pass pass) {
    order.add(createPassStep(passKey, pass));
    return this;
  }

  /**
   * Add a pass to the pass order.
   *
   * @return this
   */
  public PassOrder add(String key, Pass pass) {
    add(new PassKey(key), pass);
    return this;
  }

  /**
   * Add a pass to the pass order. The key will be generated.
   *
   * @return this
   */
  public PassOrder add(Pass pass) {
    add((PassKey) null, pass);
    return this;
  }

  /**
   * Get the list of pass steps in this pass order.
   */
  public List<PassStep> passSteps() {
    return order;
  }

  /**
   * Skips all passes in the PassOrder that are of the given class.
   */
  public PassOrder skip(Class<? extends Pass> passClass) {
    order.removeIf(s -> passClass.isInstance(s.pass()));
    return this;
  }

  /**
   * Adds a given pass after the pass with the given {@code passName}.
   */
  public PassOrder addAfterLast(Class<?> passName, Pass pass) {
    int index = -1;
    for (int i = 0; i < passSteps().size(); i++) {
      if (passName.isInstance(passSteps().get(i).pass())) {
        index = i;
      }
    }

    if (index != -1) {
      var step = createPassStep(null, pass);
      passSteps().add(index + 1, step);
    }

    return this;
  }

  /**
   * Truncates the PassOrder to only include passes until (including) the first
   * instance of the given pass class.
   * This is helpful for tests to avoid executing more passes than necessary.
   */
  public PassOrder untilFirst(Class<? extends Pass> passClass) {
    var instance = order.stream().filter(s -> passClass.isInstance(s.pass()))
        .findFirst()
        .get();
    var indexOf = order.indexOf(instance);
    order.subList(indexOf + 1, order.size()).clear();
    return this;
  }

  /**
   * Adds a new pass after each existing pass in the current pass order, excluding passes of
   * types {@link AbstractTemplateRenderingPass} and {@link ViamVerificationPass}. The additional
   * pass is determined by applying a provided function to each existing pass.
   *
   * @param passCreator a function that takes an existing pass and returns an optional pass
   *                    that should be added immediately after it. If the function returns
   *                    an empty optional, no pass is added after that specific pass.
   * @return the updated {@link PassOrder} instance, allowing for method chaining.
   */
  public PassOrder addAfterEach(Function<Pass, Optional<Pass>> passCreator) {
    var iterator = order.listIterator();

    while (iterator.hasNext()) {
      var currentPass = iterator.next();
      if (currentPass.pass() instanceof AbstractTemplateRenderingPass
          || currentPass.pass() instanceof ViamVerificationPass
      ) {
        // do not dump renderings or verifications
        continue;
      }

      passCreator
          .apply(currentPass.pass())
          .ifPresent(value -> iterator.add(createPassStep(null, value)));
    }
    return this;
  }

  /**
   * Adds a dump pass that outputs the dump to the given path.
   */
  public PassOrder addDump(String outPath) {
    var config = new GeneralConfiguration(Path.of(outPath), true);
    var last = order.getLast();
    HtmlDumpPass dumpPass = new HtmlDumpPass(HtmlDumpPass.Config.from(config,
        last.pass().getName().value(),
        "This is a dump right after the pass " + last.key().value() + "."
    ));
    add(dumpPass);
    return this;
  }

  private PassStep createPassStep(@Nullable PassKey passKey, Pass pass) {
    var currentId = passCounter.merge(pass.getClass(), 1, Integer::sum);
    if (passKey == null) {
      passKey = new PassKey(pass.getClass().getName() + "-" + currentId);
    }
    return new PassStep(passKey, pass);
  }

}
