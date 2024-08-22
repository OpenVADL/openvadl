package vadl.pass;

import java.util.LinkedHashMap;
import vadl.pass.exception.PassError;

public final class PassResults {

  private LinkedHashMap<PassKey, Object> store = new LinkedHashMap<>();

  void add(PassKey key, Object value) {
    if (store.containsKey(key)) {
      // The pipeline's steps should be deterministic.
      // If we overwrite an already existing result then it is very likely
      // that it is a bug because we schedule the same pass with the same key multiple times.
      throw new PassError(
          "Tried to store result of executed pass %s, but result for this key already exist",
          key);
    }
    store.put(key, value);
  }

  public Object get(PassKey key) {
    if (!store.containsKey(key)) {
      throw new PassError(
          ("Tried to retrieve result of executed pass %s, but result ".formatted(key)
              + "for this key does not exist. This means either a typo in the PassKey or a bug "
              + "in the pass execution order.")
      );
    }
    return store.get(key);
  }

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

  public int size() {
    return store.size();
  }


}
