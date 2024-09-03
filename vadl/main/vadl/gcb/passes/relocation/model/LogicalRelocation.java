package vadl.gcb.passes.relocation.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.Register;
import vadl.viam.Relocation;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A logical relocation is helper construct for the {@link vadl.viam.Relocation}.
 */
public class LogicalRelocation {
  public enum Kind {
    // A relocation is relative when it patches a value based on the previous value.
    // More concretely, it is relative when reads from the PC.
    RELATIVE,
    // A relocation is absolute when the patched value overwrites the previous value.
    ABSOLUTE
  }

  private final Kind kind;

  private final Format format;
  private final Relocation relocation;
  private final Format.Field immediate;

  public LogicalRelocation(@Nullable Register.Counter pc,
                           Relocation relocation,
                           Format.Field field,
                           Format format) {
    this.kind = relocation.isAbsolute(pc) ? Kind.ABSOLUTE : Kind.RELATIVE;
    this.format = format;
    this.relocation = relocation;
    this.immediate = field;
  }

  /**
   * Generate a {@link LogicalRelocation} where the relocation returns the same
   * value as input.
   */
  public LogicalRelocation(Kind kind, Format.Field immediate, Format format) {
    this.kind = kind;
    this.format = format;
    var parameter = new Parameter(new Identifier("input", SourceLocation.INVALID_SOURCE_LOCATION),
        format.type());
    this.relocation = new Relocation(format.identifier.append(".generated"),
        new Parameter[] {parameter},
        format.type());
    this.immediate = immediate;
    // Add a single return
    this.relocation.behavior().addWithInputs(new ReturnNode(new FuncParamNode(parameter)));
  }

  public Kind kind() {
    return kind;
  }

  public Format format() {
    return format;
  }

  public Relocation relocation() {
    return relocation;
  }

  public RelocationName name() {
    var suffix = format.identifier.lower() + "_" + immediate.identifier.lower();
    if (kind == Kind.ABSOLUTE) {
      return new RelocationName("ABS_" + suffix);
    } else {
      return new RelocationName("REL_" + suffix);
    }
  }

  /**
   * Get the bit slices the linker needs to patch when applying the relocation.
   */
  public Stream<Constant.BitSlice> patches() {
    return Arrays.stream(format.fieldAccesses())
        .map(fieldAccess -> fieldAccess.fieldRef().bitSlice());
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
