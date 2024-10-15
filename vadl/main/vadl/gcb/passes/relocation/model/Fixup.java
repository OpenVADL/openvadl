package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;

/**
 * Every {@link Fixup} is a {@link CompilerRelocation}. But not every {@link CompilerRelocation}
 * is a {@link Fixup}.
 */
public class Fixup {
  private final RelocationLowerable lowerable;

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

  public CppFunction valueRelocation() {
    return lowerable.valueRelocation();
  }

  public FixupName name() {
    return new FixupName(
        "fixup_" + lowerable.valueRelocation().functionName().identifier().simpleName() + "_"
            + lowerable.identifier().lower());
  }
}
