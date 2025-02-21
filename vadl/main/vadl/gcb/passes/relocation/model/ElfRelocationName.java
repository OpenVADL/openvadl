package vadl.gcb.passes.relocation.model;

import java.util.Map;
import vadl.template.Renderable;

/**
 * Value type for relocation's name.
 */
public record ElfRelocationName(String value) implements Renderable {
  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "value", value
    );
  }
}
