package vadl.gcb.passes.relocation.model;

import java.util.Map;
import java.util.Objects;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * A logical relocation is helper construct for the {@link vadl.viam.Relocation}.
 * In contrast to {@link AutomaticallyGeneratedRelocation} are {@link UserSpecifiedRelocation}
 * always user generated like {@code %lo} or {@code %hi} in risc-v.
 */
public class UserSpecifiedRelocation extends CompilerRelocation {
  private final VariantKind variantKind;

  /**
   * Constructor.
   */
  public UserSpecifiedRelocation(
      Format format,
      Format.Field field,
      Relocation originalRelocation) {
    super(generateName(format,
            field,
            originalRelocation.isAbsolute() ? Kind.ABSOLUTE : Kind.RELATIVE),
        format, field, originalRelocation);
    this.variantKind = new VariantKind(originalRelocation);
  }

  private static Identifier generateName(Format format, Format.Field imm, Kind kind) {
    return format.identifier.append(kind.name(), imm.identifier.simpleName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserSpecifiedRelocation that = (UserSpecifiedRelocation) o;
    return kind == that.kind && Objects.equals(format, that.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, format);
  }

  @Override
  public Map<String, Object> renderObj() {
    var obj = super.renderObj();
    obj.put("variantKind", variantKind);
    obj.put("name", identifier.simpleName());
    return obj;
  }
}
