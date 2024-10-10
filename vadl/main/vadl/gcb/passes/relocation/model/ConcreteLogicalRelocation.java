package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Relocation;

public class ConcreteLogicalRelocation extends LogicalRelocation implements RelocationLowerable {
  // This is the function which computes the value for the
  // relocation.
  protected final CppFunction valueRelocation;
  // This is the function which updates the value in the format.
  protected final CppFunction fieldUpdateFunction;

  /**
   * Constructor.
   *
   * @param originalRelocation
   * @param valueRelocation
   * @param field
   * @param format
   * @param fieldUpdateFunction
   * @param variantKindRef
   */
  public ConcreteLogicalRelocation(Relocation originalRelocation,
                                   CppFunction valueRelocation,
                                   Format format,
                                   Format.Field field,
                                   CppFunction fieldUpdateFunction,
                                   VariantKind variantKindRef) {
    super(originalRelocation, format, field, variantKindRef);
    this.valueRelocation = valueRelocation;
    this.fieldUpdateFunction = fieldUpdateFunction;
  }

  @Override
  public CppFunction valueRelocation() {
    return valueRelocation;
  }

  @Override
  public CppFunction fieldUpdateFunction() {
    return fieldUpdateFunction;
  }
}
