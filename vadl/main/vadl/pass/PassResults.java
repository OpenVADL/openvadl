package vadl.pass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import vadl.pass.exception.PassError;
import vadl.utils.Pair;

/**
 * Holds and maintains the pass results of all executed passes.
 * It provides an API to retrieve pass results with different properties, e.g.
 * searching by key or pass type.
 */
public final class PassResults {

  private LinkedHashMap<PassKey, Pair<Pass, Object>> store = new LinkedHashMap<>();


  /**
   * Get the result of an executed pass instance with the given pass key.
   */
  public Object get(PassKey key) {
    if (!store.containsKey(key)) {
      throw new PassError(
          ("Tried to retrieve result of executed pass %s, but result ".formatted(key)
              + "for this key does not exist. This means either a typo in the PassKey or a bug "
              + "in the pass execution order.")
      );
    }
    return store.get(key).right();
  }

  /**
   * Get the result of an executed pass instance with the given pass key.
   * It will cast the result to the given type, and will ensure that the type is correct.
   */
  public <T> T get(PassKey key, Class<T> type) {
    var result = get(key);
    if (!type.isInstance(result)) {
      throw new PassError(
          "Result of executed pass %s, with expected type %s was tried ".formatted(key, type)
              + "to retrieve. But result's actual type is %s.".formatted(result.getClass()));
    }
    //noinspection unchecked
    return (T) result;
  }

  /**
   * Retrieves the pass result of the last execution of the given passClass.
   * This allows searching for the result of a pass type instead of one with a specific key.
   *
   * @param passClass the class of the Pass to retrieve the result for
   * @return an empty option if no pass instance of the passClass was executed. Otherwise,
   *     the pass result wrapped in the optional.
   */
  public <T> Optional<?> getOfLastExecution(Class<T> passClass) {
    var result =
        store.values().stream()
            .reduce((a, b) -> passClass.isInstance(b.left()) ? b : a);
    if (result.isEmpty()) {
      return Optional.empty();
    }
    var resultPair = result.get();
    if (passClass.isInstance(resultPair.left())) {
      return Optional.of(resultPair.right());
    }
    return Optional.empty();
  }

  /**
   * Returns a list of all executed passes in the executed order.
   */
  public List<Pass> executedPasses() {
    return store.values().stream().map(Pair::left).toList();
  }

  public int size() {
    return store.size();
  }

  // this is only visible on package level to ensure that passes can't manipulate pass results
  void add(PassKey key, Pass pass, Object value) {
    if (store.containsKey(key)) {
      // The pipeline's steps should be deterministic.
      // If we overwrite an already existing result then it is very likely
      // that it is a bug because we schedule the same pass with the same key multiple times.
      throw new PassError(
          "Tried to store result of executed pass %s, but result for this key already exist",
          key);
    }
    store.put(key, Pair.of(pass, value));
  }


  public static PassResults empty() {
    return new PassResults();
  }

}
