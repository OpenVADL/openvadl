package vadl.cppCodeGen.model;

import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Kind for {@link LogicalRelocation} and immediates.
 */
public record VariantKind(String value) {
  public VariantKind(Format.Field field) {
    this("VK_" + field.identifier.lower());
  }

  public VariantKind(Relocation relocation) {
    this("VK_" + relocation.identifier.lower());
  }
}
