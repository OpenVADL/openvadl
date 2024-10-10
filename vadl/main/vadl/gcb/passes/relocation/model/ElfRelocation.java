package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Wrapper for {@link LogicalRelocation}.
 */
public class ElfRelocation {
  private final RelocationLowerable lowerable;

  public ElfRelocation(RelocationLowerable lowerable) {
    this.lowerable = lowerable;
  }

  public Format format() {
    return lowerable.format();
  }

  public LogicalRelocation.Kind kind() {
    return lowerable.kind();
  }

  public Relocation relocation() {
    return lowerable.relocation();
  }

  public CppFunction valueRelocation() {
    return lowerable.valueRelocation();
  }

  public CppFunction fieldUpdateFunction() {
    return lowerable.fieldUpdateFunction();
  }

  public VariantKind variantKind() {
    return lowerable.variantKind();
  }

  public ElfRelocationName name() {
    return new ElfRelocationName(
        "R_" + lowerable.relocation().identifier.lower());
  }
}
