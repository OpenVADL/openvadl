package vadl.cppCodeGen.model;

import java.util.Map;
import vadl.template.Renderable;

/**
 * Value wrapper for class name.
 */
public record CppClassImplName(String name) implements Renderable {
  public String lower() {
    return name;
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "name", name,
        "lower", lower()
    );
  }
}
