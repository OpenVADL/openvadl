package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * {@link CompilerRelocation} is a super class to hold
 * both {@link LogicalRelocation} and {@link GeneratedRelocation}.
 */
public abstract class CompilerRelocation {
  protected final CompilerRelocation.Kind kind;
  protected final Format format;
  protected final Format.Field immediate;
  protected final Relocation relocationRef;
  protected final VariantKind variantKindRef;

  /**
   * Determines what kind of relocation this is.
   * A relocation is relative when it patches a value based on the previous value.
   * More concretely, it is relative when reads from the PC.
   * A relocation is absolute when the patched value overwrites the previous value.
   */
  public enum Kind {
    RELATIVE,
    ABSOLUTE;

    /**
     * Returns {@code true} when kind is {@code RELATIVE}.
     */
    public boolean isRelative() {
      return this == RELATIVE;
    }
  }

  /**
   * Constructor.
   */
  public CompilerRelocation(Format format,
                            Format.Field immediate,
                            Relocation relocationRef,
                            VariantKind variantKindRef
  ) {
    this(relocationRef.isAbsolute() ? Kind.ABSOLUTE : Kind.RELATIVE,
        format,
        immediate,
        relocationRef,
        variantKindRef);
  }

  /**
   * Constructor.
   */
  public CompilerRelocation(
      Kind kind,
      Format format,
      Format.Field immediate,
      Relocation relocationRef,
      VariantKind variantKindRef
  ) {
    this.kind = kind;
    this.format = format;
    this.relocationRef = relocationRef;
    this.variantKindRef = variantKindRef;
    this.immediate = immediate;
  }

  public Kind kind() {
    return kind;
  }

  public Format format() {
    return format;
  }

  public Relocation relocation() {
    return relocationRef;
  }

  public Format.Field immediate() {
    return immediate;
  }

  /**
   * Get the ELF name.
   */
  public ElfRelocationName elfRelocationName() {
    return new ElfRelocationName(
        "R_" + relocation().identifier.lower());
  }

  public VariantKind variantKind() {
    return variantKindRef;
  }
}
