package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * Helper interface for the concrete relocation implementations.
 */
public interface RelocationLowerable {
  /**
   * Get the identifier of the relocation.
   */
  Identifier identifier();

  /**
   * Get the kind of the relocation.
   */
  CompilerRelocation.Kind kind();

  /**
   * Get the variant kind of the relocation.
   */
  VariantKind variantKind();

  /**
   * Get the cpp function for changing a value for a relocation.
   */
  CppFunction valueRelocation();

  /**
   * Get the cpp function for updating a field in a format.
   */
  CppFunction fieldUpdateFunction();

  /**
   * Get the ELF name.
   */
  ElfRelocationName elfRelocationName();
}
