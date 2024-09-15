package vadl.gcb.passes.relocation.model;

import java.util.Objects;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.utils.SourceLocation;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.Relocation;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A logical relocation is helper construct for the {@link vadl.viam.Relocation}.
 */
public class LogicalRelocation {
  /**
   * Determines what kind of relocation this is.
   * A relocation is relative when it patches a value based on the previous value.
   * More concretely, it is relative when reads from the PC.
   * A relocation is absolute when the patched value overwrites the previous value.
   */
  public enum Kind {
    RELATIVE,
    ABSOLUTE
  }

  private final Identifier identifier;
  private final Kind kind;

  private final Format format;
  private final Relocation relocation;
  private final CppFunction cppRelocation;
  private final CppFunction updateFunction;
  private final VariantKind variantKind;

  /**
   * Constructor.
   */
  public LogicalRelocation(
      Relocation originalRelocation,
      CppFunction relocation,
      Format.Field field,
      Format format,
      CppFunction updateFunction) {
    this.relocation = originalRelocation;
    this.kind = originalRelocation.isAbsolute() ? Kind.ABSOLUTE : Kind.RELATIVE;
    this.identifier = generateName(format, field, kind);
    this.format = format;
    this.cppRelocation = relocation;
    this.updateFunction = updateFunction;
    this.variantKind = new VariantKind(originalRelocation);
  }

  /**
   * Generate a {@link LogicalRelocation} where the relocation returns the same
   * value as input.
   */
  public LogicalRelocation(Kind kind,
                           Format.Field immediate,
                           Format format,
                           CppFunction updateFunction) {
    this.kind = kind;
    this.updateFunction = updateFunction;
    this.format = format;
    var parameter = new Parameter(new Identifier("input", SourceLocation.INVALID_SOURCE_LOCATION),
        format.type());
    this.identifier = generateName(format, immediate, kind);
    this.relocation = new Relocation(identifier,
        new Parameter[] {parameter},
        format.type());
    this.cppRelocation = new CppFunction(relocation);
    // Add a single return
    this.cppRelocation.behavior().addWithInputs(new ReturnNode(new FuncParamNode(parameter)));
    this.variantKind = new VariantKind(relocation);
  }

  private Identifier generateName(Format format, Format.Field imm, Kind kind) {
    return format.identifier.append(kind.name(), imm.identifier.simpleName());
  }

  public Identifier identifier() {
    return identifier;
  }

  public Kind kind() {
    return kind;
  }

  public Format format() {
    return format;
  }

  /**
   * Get relocation.
   */
  public Relocation relocation() {
    return relocation;
  }

  public CppFunction cppRelocation() {
    return cppRelocation;
  }

  public CppFunction updateFunction() {
    return updateFunction;
  }

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
