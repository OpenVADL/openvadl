package vadl.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ClassUtils;
import vadl.utils.Pair;

/**
 * Normalizes {@link Renderable} variables and checks them
 * to be one of {@code Map, List, String} and primitives (+ Wrappers).
 * This is necessary to provide reliable native images, as rendering of templates requires
 * reflective access to variables.
 * While java.* classes are natively supported by GraalVM, all custom classes would have to be
 * registered for reflection.
 * This is not trivial, and therefore we limit the types that can be rendered to this small set.
 */
class VariableNormalizer {

  static Map<String, Object> normalizeAndCheckVariables(Map<String, Object> variables)
      throws IllegalRenderTypeException {
    var entriesToPut = new ArrayList<Pair<String, Object>>();
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      var newVal = normalizeAndCheck(entry.getValue());
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

  private static @Nullable Object normalizeAndCheck(@Nullable Object object)
      throws IllegalRenderTypeException {
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
      return normalizeAndCheck(map);
    }
    if (object instanceof List<?> list) {
      return normalizeAndCheck(list);
    }
    if (object instanceof Renderable renderable) {
      return normalizeAndCheck(renderable.renderObj());
    }
    throw new IllegalRenderTypeException(
        "Unsupported rendering type: " + object + " of type " + object.getClass());
  }

  private static Object normalizeAndCheck(Map<?, ?> map) throws IllegalRenderTypeException {
    var newMap = new HashMap<String, Object>();
    for (var entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new IllegalRenderTypeException(
            "All map keys that are getting rendered must be a String. Found: " + entry.getKey());
      }
      newMap.put(key, normalizeAndCheck(entry.getValue()));
    }
    return newMap;
  }

  private static Object normalizeAndCheck(List<?> map) {
    return map.stream().map(VariableNormalizer::normalizeAndCheck)
        .collect(Collectors.toCollection(ArrayList::new));
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
