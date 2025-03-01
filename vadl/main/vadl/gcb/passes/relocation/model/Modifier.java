package vadl.gcb.passes.relocation.model;

import java.util.Map;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Represents the transformation functions(?) in the assembler during fixups.
 */
public record Modifier(String value) implements Renderable {
  public static Modifier from(Relocation relocation, Format.Field field) {
    var name =  relocation.identifier.lower() + "_" + field.identifier.tail().lower();
    return new Modifier("MO_" + name);
  }

  public static Modifier absolute(Format.Field imm) {
    return new Modifier("MO_ABS_" + imm.identifier.lower());
  }

  public static Modifier relative(Format.Field imm) {
    return new Modifier("MO_REL_" + imm.identifier.lower());
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of("value", value);
  }
}
