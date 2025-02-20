package vadl.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ClassUtils;
import vadl.utils.Pair;

class VariableChecker {

  static Map<String, Object> checkVariables(Map<String, Object> variables)
      throws IllegalRenderTypeException {
    var entriesToPut = new ArrayList<Pair<String, Object>>();
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      var newVal = check(entry.getValue());
      if (newVal != null && newVal != entry.getValue()) {
        entriesToPut.add(new Pair<>(entry.getKey(), newVal));
      }
    }
    if (!entriesToPut.isEmpty()) {
      // copy as map might be immutable
      variables = new HashMap<>(variables);
    }
    for (var entry : entriesToPut) {
      variables.put(entry.left(), entry.right());
    }
    return variables;
  }

  private static @Nullable Object check(@Nullable Object object) throws IllegalRenderTypeException {
    if (object == null) {
      return null;
    }
    if (ClassUtils.isPrimitiveOrWrapper(object.getClass())) {
      return object;
    }
    if (object instanceof String) {
      return object;
    }
    if (object instanceof Map<?, ?> map) {
      return check(map);
    }
    if (object instanceof List<?> list) {
      return check(list);
    }
    if (object instanceof Renderable renderable) {
      return check(renderable.renderObj());
    }
    throw new IllegalRenderTypeException(
        "Unsupported rendering type: " + object + " of type " + object.getClass());
  }

  private static Object check(Map<?, ?> map) throws IllegalRenderTypeException {
    var newMap = new HashMap<String, Object>();
    for (var entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new IllegalRenderTypeException(
            "All map keys that are getting rendered must be a String. Found: " + entry.getKey());
      }
      newMap.put(key, check(entry.getValue()));
    }
    return newMap;
  }

  private static Object check(List<?> map) {
    return map.stream().map(VariableChecker::check).toList();
  }
}

class IllegalRenderTypeException extends RuntimeException {
  public IllegalRenderTypeException() {
  }

  public IllegalRenderTypeException(String message) {
    super(message);
  }

  public IllegalRenderTypeException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalRenderTypeException(Throwable cause) {
    super(cause);
  }
}
