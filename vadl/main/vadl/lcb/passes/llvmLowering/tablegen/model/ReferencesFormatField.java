package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.Format;
import vadl.viam.Format.Field;

/**
 * Indicator interface which indicates that the operand is referencing
 * a {@link Field}.
 */
public interface ReferencesFormatField {
  /**
   * Get the field from the operand.
   */
  Format.Field formatField();
}
