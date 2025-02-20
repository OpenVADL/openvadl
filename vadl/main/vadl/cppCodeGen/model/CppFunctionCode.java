package vadl.cppCodeGen.model;

import java.util.Map;
import vadl.template.Renderable;

/**
 * Value type for code.
 */
public record CppFunctionCode(String value) implements Renderable {
  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "value", value
    );
  }
}
