package vadl.gcb.passes.relocation.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Register;
import vadl.viam.Relocation;

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

  public LogicalRelocation(Register.Counter pc, Relocation relocation, Format format) {
    this.kind = relocation.isAbsolute(pc) ? Kind.ABSOLUTE : Kind.RELATIVE;
    this.format = format;
  }

  public LogicalRelocation(Kind kind, Format format) {
    this.kind = kind;
    this.format = format;
  }

  public Kind kind() {
    return kind;
  }

  public Format format() {
    return format;
  }

  public RelocationName name() {
    if (kind == Kind.ABSOLUTE) {
      return new RelocationName("ABS_" + format.identifier.lower());
    } else {
      return new RelocationName("REL_" + format.identifier.lower());
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
