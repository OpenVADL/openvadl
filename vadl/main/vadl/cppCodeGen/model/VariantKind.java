package vadl.cppCodeGen.model;

import java.util.Map;
import vadl.gcb.passes.relocation.model.UserSpecifiedRelocation;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * A {@link VariantKind} specifies how a symbol get referenced. It can be over a
 * {@link UserSpecifiedRelocation} or GOT or more.
 */
public record VariantKind(String value, String human, boolean isImmediate) implements Renderable {
  public VariantKind(Relocation relocation) {
    this("VK_" + relocation.identifier.lower(), relocation.identifier.simpleName(), false);
  }

  public static VariantKind none() {
    return new VariantKind("VK_None", "None", false);
  }

  public static VariantKind invalid() {
    return new VariantKind("VK_Invalid", "Invalid", false);
  }


  /**
   * Create an absolute variant kind.
   */
  public static VariantKind absolute(Relocation relocation, Format.Field field) {
    var name = relocation.identifier.lower() + "_" + field.identifier.tail().lower();

    return new VariantKind("VK_ABS_" + name,
        "ABS_" + name, false);
  }

  /**
   * Create an absolute variant kind.
   */
  public static VariantKind absolute(Format.Field field) {
    return new VariantKind("VK_SYMB_ABS_" + field.identifier.lower(),
        "SYMB_ABS_" + field.identifier.lower(), true);
  }

  /**
   * Create a relative variant kind.
   */
  public static VariantKind relative(Relocation relocation, Format.Field field) {
    var name = relocation.identifier.lower() + "_" + field.identifier.tail().lower();

    return new VariantKind("VK_PCREL_" + name,
        "PCREL_" + name, false);
  }

  /**
   * Create a relative variant kind.
   */
  public static VariantKind relative(Format.Field field) {
    return new VariantKind("VK_SYMB_PCREL_" + field.identifier.lower(),
        "SYMB_PCREL_" + field.identifier.lower(), true);
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
