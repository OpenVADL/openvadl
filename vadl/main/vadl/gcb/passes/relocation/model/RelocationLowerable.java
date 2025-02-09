package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;

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
  GcbImmediateExtractionCppFunction valueRelocation();

  /**
   * Get the cpp function for updating a field in a format.
   */
  GcbUpdateFieldRelocationCppFunction fieldUpdateFunction();

  /**
   * Get the ELF name.
   */
  ElfRelocationName elfRelocationName();

  /**
   * Get the format.
   */
  Format format();
}
