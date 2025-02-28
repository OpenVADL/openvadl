package vadl.gcb.passes.relocation.model;

import java.util.HashMap;
import java.util.Map;
import vadl.cppCodeGen.model.VariantKind;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * {@link CompilerRelocation} is a super class to hold
 * both {@link UserSpecifiedRelocation} and {@link AutomaticallyGeneratedRelocation}.
 */
public abstract class CompilerRelocation implements Renderable {
  protected final Identifier identifier;
  protected final CompilerRelocation.Kind kind;
  protected final Format format;
  protected final Format.Field immediate;
  protected final Relocation relocationRef;

  /**
   * Determines what kind of relocation this is.
   * A relocation is relative when it patches a value based on the previous value.
   * More concretely, it is relative when reads from the PC.
   * A relocation is absolute when the patched value overwrites the previous value.
   */
  public enum Kind implements Renderable {
    RELATIVE,
    ABSOLUTE;

    /**
     * Returns {@code true} when kind is {@code RELATIVE}.
     */
    public boolean isRelative() {
      return this == RELATIVE;
    }

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", this.name(),
          "isRelative", this.isRelative()
      );
    }
  }

  /**
   * Constructor.
   */
  public CompilerRelocation(
      Identifier identifier,
      Format format,
      Format.Field immediate,
      Relocation relocationRef
  ) {
    this(identifier,
        relocationRef.isAbsolute() ? Kind.ABSOLUTE : Kind.RELATIVE,
        format,
        immediate,
        relocationRef);
  }

  /**
   * Constructor.
   */
  public CompilerRelocation(
      Identifier identifier,
      Kind kind,
      Format format,
      Format.Field immediate,
      Relocation relocationRef
  ) {
    this.identifier = identifier;
    this.kind = kind;
    this.format = format;
    this.relocationRef = relocationRef;
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

  public Identifier identifier() {
    return identifier;
  }

  /**
   * Get the ELF name.
   */
  public ElfRelocationName elfRelocationName() {
    return new ElfRelocationName(
        "R_" + relocation().identifier.lower());
  }

  @Override
  public Map<String, Object> renderObj() {
    var obj = new HashMap<String, Object>();
    obj.put("kind", kind().name());
    obj.put("elfRelocationName", elfRelocationName());
    obj.put("relocation", Map.of(
        "name", relocation().simpleName()
    ));
    return obj;
  }
}
