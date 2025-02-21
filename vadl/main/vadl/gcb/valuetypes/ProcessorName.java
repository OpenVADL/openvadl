package vadl.gcb.valuetypes;

import java.util.Map;
import vadl.template.Renderable;

/**
 * Name of the processor.
 */
public record ProcessorName(String value) implements Renderable {
  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "value", value
    );
  }
}
