package vadl.gcb.passes.relocation.model;

import java.util.Objects;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * A logical relocation is helper construct for the {@link vadl.viam.Relocation}.
 * In contrast to {@link GeneratedRelocation} are {@link LogicalRelocation} always
 * user generated like {@code %lo} or {@code %hi} in risc-v.
 */
public class LogicalRelocation extends CompilerRelocation {
  private final Identifier identifier;
  private final VariantKind variantKind;

  /**
   * Constructor.
   */
  public LogicalRelocation(
      Relocation originalRelocation,
      Format format,
      Format.Field field,
      VariantKind variantKindRef) {
    super(format, field, originalRelocation, variantKindRef);
    this.identifier = generateName(format, field, kind);
    this.variantKind = new VariantKind(originalRelocation);
  }

  private Identifier generateName(Format format, Format.Field imm, Kind kind) {
    return format.identifier.append(kind.name(), imm.identifier.simpleName());
  }

  public Identifier identifier() {
    return identifier;
  }


  @Override
  public VariantKind variantKind() {
    return variantKind;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LogicalRelocation that = (LogicalRelocation) o;
    return kind == that.kind && Objects.equals(format, that.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, format);
  }
}
