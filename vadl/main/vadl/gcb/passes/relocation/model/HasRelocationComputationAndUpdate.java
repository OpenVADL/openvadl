package vadl.gcb.passes.relocation.model;

import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.viam.Identifier;

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
   * Get the cpp function for changing a value for a relocation.
   */
  GcbImmediateExtractionCppFunction valueRelocation();

  /**
   * Get the cpp function for updating a field in a format.
   */
  GcbUpdateFieldRelocationCppFunction fieldUpdateFunction();
}
