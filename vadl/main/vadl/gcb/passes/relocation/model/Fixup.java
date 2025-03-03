package vadl.gcb.passes.relocation.model;

/**
 * Every {@link Fixup} is like a {@link CompilerRelocation}. But not every
 * {@link CompilerRelocation} is a {@link Fixup}. The compiler can resolve symbols in the same
 * compilation unit. During a limited time, when it is not clear whether it can be resolved or not,
 * it is {@link Fixup}. Later it will be mapped into a {@link CompilerRelocation} when the
 * address is only known at compile-time.
 */
public class Fixup {
  private final CompilerRelocation.Kind kind;
  private final HasRelocationComputationAndUpdate implementedRelocation;

  /**
   * Create a fixup for relocations which have been generated for an immediate.
   */
  public Fixup(AutomaticallyGeneratedRelocation automaticallyGeneratedRelocation) {
    this.kind = automaticallyGeneratedRelocation.kind;
    this.implementedRelocation = automaticallyGeneratedRelocation;
  }

  /**
   * Create a fixup for relocations which have been specified from a user.
   */
  public Fixup(ImplementedUserSpecifiedRelocation implementedUserSpecifiedRelocation) {
    this.kind = implementedUserSpecifiedRelocation.kind;
    this.implementedRelocation = implementedUserSpecifiedRelocation;
  }

  /**
   * Get the name of the fixup.
   */
  public FixupName name() {
    return new FixupName(
        "fixup_"
            + implementedRelocation.valueRelocation().functionName().identifier().simpleName() + "_"
            + implementedRelocation.identifier().lower());
  }

  public CompilerRelocation.Kind kind() {
    return kind;
  }

  public HasRelocationComputationAndUpdate implementedRelocation() {
    return implementedRelocation;
  }
}
