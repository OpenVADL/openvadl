package vadl.cppCodeGen.model;

import java.util.Map;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Kind for {@link LogicalRelocation} and immediates.
 */
public record VariantKind(String value, String human, boolean isImmediate) implements Renderable {
  public VariantKind(Format.Field field) {
    this("VK_" + field.identifier.lower(), field.identifier.lower(), true);
  }

  public VariantKind(Relocation relocation) {
    this("VK_" + relocation.identifier.lower(), relocation.identifier.simpleName(), false);
  }

  public static VariantKind none() {
    return new VariantKind("VK_None", "None", false);
  }

  public static VariantKind invalid() {
    return new VariantKind("VK_Invalid", "Invalid", false);
  }

  public static VariantKind absolute(Format.Field field) {
    return new VariantKind("VK_SYMB_ABS_" + field.identifier.lower(),
        "SYMB_ABS_" + field.identifier.lower(), false);
  }

  public static VariantKind relative(Format.Field field) {
    return new VariantKind("VK_SYMB_PCREL_" + field.identifier.lower(),
        "SYMB_PCREL_" + field.identifier.lower(), false);
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "value", value,
        "human", human,
        "isImmediate", isImmediate
    );
  }
}
