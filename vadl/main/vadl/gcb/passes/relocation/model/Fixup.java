package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.VariantKind;

/**
 * Every {@link Fixup} is like a {@link CompilerRelocation}. But not every
 * {@link CompilerRelocation} is a {@link Fixup}. The compiler can resolve symbols in the same
 * compilation unit. During a limited time, when it is not clear whether it can be resolved or not,
 * it is {@link Fixup}. Later it will be mapped into a {@link CompilerRelocation} when the
 * address is only known at compile-time.
 */
public class Fixup {
  private final RelocationLowerable lowerable;

  /**
   * Constructor.
   */
  public Fixup(RelocationLowerable lowerable) {
    this.lowerable = lowerable;
  }

  public CompilerRelocation.Kind kind() {
    return lowerable.kind();
  }

  public VariantKind variantKind() {
    return lowerable.variantKind();
  }

  public RelocationLowerable relocationLowerable() {
    return lowerable;
  }

  public GcbImmediateExtractionCppFunction valueRelocation() {
    return lowerable.valueRelocation();
  }

  /**
   * Get the name of the fixup.
   */
  public FixupName name() {
    return new FixupName(
        "fixup_" + lowerable.valueRelocation().functionName().identifier().simpleName() + "_"
            + lowerable.identifier().lower());
  }
}
