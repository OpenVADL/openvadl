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

  public static VariantKind absolute() {
    return new VariantKind("VK_SYMB_ABS", "_SYMB_ABS");
  }

  public static VariantKind relative() {
    return new VariantKind("VK_SYMB_PCREL", "_SYMB_PCREL");
  }
}
