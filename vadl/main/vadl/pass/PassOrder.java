// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.pass;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.HtmlDumpPass;

/**
 * This class defines the order in which the {@link PassManager} should run them.
 */
public final class PassOrder {

  // a counter-map that keeps track of how many passes of each pass class exists.
  // this is used to generate a unique pass key if it is not given by the user.
  private static final Map<Class<? extends Pass>, Integer> passCounter
      = new ConcurrentHashMap<>();

  // the actual list of pass steps
  // we use a linked list, as we add passes in between
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

  public PassOrder addAfterFirst(Class<?> selector, Pass pass) {
    int index = -1;
    var i = 0;
    for (PassStep step : order) {
      if (selector.isInstance(step.pass())) {
        index = i;
        break;
      }
      i++;
    }

    if (index != -1) {
      var step = createPassStep(null, pass);
      passSteps().add(index + 1, step);
    }

    return this;
  }


  /**
   * Adds a given pass after the selector pass.
   */
  public PassOrder addAfterLast(Class<?> selector, Pass pass) {
    int index = -1;
    for (int i = 0; i < passSteps().size(); i++) {
      if (selector.isInstance(passSteps().get(i).pass())) {
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
   * Adds a new pass between all existing passes in the current pass order.
   * The additional pass is determined by applying a provided function to each existing pass and
   * its next pass.
   * The last pass, that hasn't a next pass, is also passed to the creator
   * with a next pass of {@code Optional.empty()}.
   *
   * @param passCreator a function that takes an existing pass and its next (optional) pass,
   *                    and returns an optional pass that should be added between the given passes.
   *                    If the function returns an empty optional, no pass is added.
   * @return the updated {@link PassOrder} instance, allowing for method chaining.
   */
  public PassOrder addBetweenEach(BiFunction<Pass, Optional<Pass>, Optional<Pass>> passCreator) {
    var iterator = order.listIterator();
    if (!iterator.hasNext()) {
      return this;
    }

    while (iterator.hasNext()) {
      var current = iterator.next().pass();

      Optional<Pass> next = Optional.empty();
      if (iterator.hasNext()) {
        // peek next element
        next = Optional.of(iterator.next().pass());
        iterator.previous();
      }

      passCreator
          .apply(current, next)
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
