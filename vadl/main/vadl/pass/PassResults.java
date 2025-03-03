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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.pass.exception.PassError;

/**
 * Holds and maintains the pass results of all executed passes.
 * It provides an API to retrieve pass results with different properties, e.g.
 * searching by key or pass type.
 */
public final class PassResults {

  private LinkedHashMap<PassKey, SingleResult> store = new LinkedHashMap<>();


  /**
   * Get the result of an executed pass instance with the given pass key.
   */
  public @Nullable Object get(PassKey key) {
    if (!store.containsKey(key)) {
      throw new PassError(
          ("Tried to retrieve result of executed pass %s, but result ".formatted(key)
              + "for this key does not exist. This means either a typo in the PassKey or a bug "
              + "in the pass execution order.")
      );
    }
    return store.get(key).result();
  }

  /**
   * Get the result of an executed pass instance with the given pass key.
   * It will cast the result to the given type, and will ensure that the type is correct.
   */
  public <T> T get(PassKey key, Class<T> type) {
    var result = get(key);
    if (result == null) {
      throw new PassError("Tried to retrieve result of executed pass %s, but result was null.",
          key);
    }
    if (!type.isInstance(result)) {
      throw new PassError(
          "Result of executed pass %s, with expected type %s was tried ".formatted(key, type)
              + "to retrieve. But result's actual type is %s.".formatted(result.getClass()));
    }
    //noinspection unchecked
    return (T) result;
  }

  /**
   * Retrieves the pass instance as {@link SingleResult} object of the last execution of the
   * given passClass.
   * This allows searching for the result of a pass type instead of one with a specific key.
   *
   * @param passClass the class of the Pass to retrieve the result for
   * @return the result of the found pass
   */
  public <T extends Pass> SingleResult lastExecutionOf(Class<T> passClass) {
    var result =
        store.values().stream()
            .reduce((a, b) -> passClass.isInstance(b.pass()) ? b : a);
    if (result.isEmpty() || !passClass.isInstance(result.get().pass)) {
      throw new PassError(
          "Tried to retrieve result of the last instance of pass class %s, ".formatted(passClass)
              + "but no such pass instance was found.");
    }
    return result.get();
  }

  public SingleResult lastExecution() {
    return store.values().toArray(SingleResult[]::new)[store.size() - 1];
  }

  /**
   * Retrieves the pass result of the last execution of the given passClass.
   * This allows searching for the result of a pass type instead of one with a specific key.
   * If the pass result is null, it will throw an error.
   *
   * @param passClass the class of the Pass to retrieve the result for
   * @return the result of the found pass
   */
  public <T extends Pass> Object lastResultOf(Class<T> passClass) {
    var result = lastNullableResultOf(passClass);
    if (result == null) {
      throw new PassError(
          "Expected that result of last instance of pass %s is not null, but it was null".formatted(
              passClass));
    }
    return result;
  }

  public <T extends Pass, R> R lastResultOf(Class<T> passClass, Class<R> type) {
    var result = lastResultOf(passClass);
    return type.cast(result);
  }


  /**
   * Retrieves the pass result of the last execution of the given passClass.
   * This allows searching for the result of a pass type instead of one with a specific key.
   * In contrast to {@link #lastResultOf(Class)} the result might be null.
   *
   * @param passClass the class of the Pass to retrieve the result for
   * @return the result of the found pass
   */
  public <T extends Pass> @Nullable Object lastNullableResultOf(Class<T> passClass) {
    return lastExecutionOf(passClass).result();
  }

  public <T extends Pass, R> Stream<R> allResultsOf(Class<T> passClass, Class<R> type) {
    return allExecutionsOf(passClass).map(SingleResult::result).map(type::cast);
  }

  public <T extends Pass> Stream<SingleResult> allExecutionsOf(Class<T> passClass) {
    return executedPasses().stream().filter(x -> passClass.isInstance(x.pass));
  }

  /**
   * Returns a list of all executed passes in the executed order.
   */
  public List<SingleResult> executedPasses() {
    return store.values().stream().toList();
  }

  /**
   * Checks whether a pass is at least run once.
   */
  public boolean hasRunPassOnce(Class<?> pass) {
    return store.values().stream().anyMatch(x -> pass.isInstance(x.pass));
  }

  public int size() {
    return store.size();
  }

  // this is only visible on package level to ensure that passes can't manipulate pass results
  void add(PassKey key, Pass pass, long durationMs, @Nullable Object result) {
    if (store.containsKey(key)) {
      // The pipeline's steps should be deterministic.
      // If we overwrite an already existing result then it is very likely
      // that it is a bug because we schedule the same pass with the same key multiple times.
      throw new PassError(
          "Tried to store result of executed pass %s, but result for this key already exist",
          key);
    }
    store.put(key, new SingleResult(key, pass, durationMs, result));
  }


  public static PassResults empty() {
    return new PassResults();
  }

  /**
   * Holds all components of a finished pass execution, namely the unique {@link PassKey},
   * the actual {@link Pass} instance and the result object from the pass execution.
   */
  public record SingleResult(
      PassKey passKey,
      Pass pass,
      long durationMs,
      @Nullable Object result
  ) {
  }

}
