package vadl.cppCodeGen.model;

import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Kind for {@link LogicalRelocation} and immediates.
 */
public record VariantKind(String value, String human) {
  public VariantKind(Format.Field field) {
    this("VK_" + field.identifier.lower(), field.identifier.lower());
  }

  public VariantKind(Relocation relocation) {
    this("VK_" + relocation.identifier.lower(), relocation.identifier.simpleName());
  }

  public static VariantKind none() {
    return new VariantKind("VK_None", "None");
  }

  public static VariantKind invalid() {
    return new VariantKind("VK_Invalid", "Invalid");
  }

  public static VariantKind absolute(Format.Field field) {
    return new VariantKind("VK_SYMB_ABS_" + field.identifier.lower(),
        "SYMB_ABS_" + field.identifier.lower());
  }

  public static VariantKind relative(Format.Field field) {
    return new VariantKind("VK_SYMB_PCREL_" + field.identifier.lower(),
        "SYMB_PCREL_" + field.identifier.lower());
  }
}
