package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * The {@link Fixup} requires already implemented functions. But there are two kinds which are
 * relevant: {@link ImplementedUserSpecifiedRelocation} and
 * {@link AutomaticallyGeneratedRelocation}.
 */
public interface HasRelocationComputationAndUpdate {

  /**
   * Get the identifier of the function.
   */
  Identifier identifier();

  /**
   * Get the {@link VariantKind} for the relocation.
   */
  VariantKind variantKind();

  /**
   * Get the {@link Format} on which the relocation should be applied on.
   */
  Format format();

  /**
   * Get the relocation.
   */
  Relocation relocation();

  /**
   * Get the cpp function for changing a value for a relocation.
   */
  GcbImmediateExtractionCppFunction valueRelocation();

  /**
   * Get the cpp function for updating a field in a format.
   */
  GcbUpdateFieldRelocationCppFunction fieldUpdateFunction();

  /**
   * Generates and returns the name of the ELF relocation.
   */
  ElfRelocationName elfRelocationName();
}
