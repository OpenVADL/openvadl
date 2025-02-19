package vadl.template;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ClassUtils;
import vadl.viam.Specification;

class VariableChecker {

  static void checkVariables(Map<String, Object> variables) {
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      check(entry.getValue());
    }
  }

  private static void check(@Nullable Object object) {
    if (object == null) {
      return;
    }
    if (ClassUtils.isPrimitiveOrWrapper(object.getClass())) {
      return;
    }
    if (object instanceof String) {
      return;
    }
    if (object instanceof Map<?, ?> map) {
      check(map);
      return;
    }
    if (object instanceof List<?> list) {
      check(list);
      return;
    }
    throw new IllegalArgumentException(
        "Unsupported rendering type: " + object + " of type " + object.getClass());
  }

  private static void check(Map<?, ?> map) {
    for (var entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String)) {
        throw new IllegalArgumentException(
            "All map keys that are getting rendered must be a String. Found: " + entry.getKey());
      }
      check(entry.getValue());
    }
  }

  private static void check(List<?> map) {
    for (var object : map) {
      check(object);
    }
  }


}
