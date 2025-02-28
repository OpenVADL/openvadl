package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * A concrete logical relocation is like a logical relocation (lo, hi) but it already
 * has the cpp functions for the compiler backend.
 */
public class ImplementedUserSpecifiedRelocation extends UserSpecifiedRelocation implements RelocationLowerable {
  // This is the function which computes the value for the
  // relocation.
  protected final GcbImmediateExtractionCppFunction valueRelocation;
  // This is the function which updates the value in the format.
  protected final GcbUpdateFieldRelocationCppFunction fieldUpdateFunction;

  /**
   * Constructor.
   */
  public ImplementedUserSpecifiedRelocation(Relocation originalRelocation,
                                            GcbImmediateExtractionCppFunction valueRelocation,
                                            Format format,
                                            Format.Field field,
                                            GcbUpdateFieldRelocationCppFunction fieldUpdateFunction,
                                            VariantKind variantKindRef) {
    super(originalRelocation, format, field, variantKindRef);
    this.valueRelocation = valueRelocation;
    this.fieldUpdateFunction = fieldUpdateFunction;
  }

  @Override
  public GcbImmediateExtractionCppFunction valueRelocation() {
    return valueRelocation;
  }

  @Override
  public GcbUpdateFieldRelocationCppFunction fieldUpdateFunction() {
    return fieldUpdateFunction;
  }

  @Override
  public ElfRelocationName elfRelocationName() {
    return new ElfRelocationName(
        "R_" + relocation().identifier.lower() + "_"
            + format.identifier.simpleName()
            + "_" + immediate.identifier.simpleName()
    );
  }
}
