package vadl.gcb.passes.encoding.strategies;

import vadl.viam.Format;
import vadl.viam.Parameter;

/**
 * The implementor of this interface can generate a field access encoding function.
 */
public interface EncodingGenerationStrategy {
  /**
   * Check if the strategy can be applied. Returns {@code true} when it is applicable.
   */
  boolean checkIfApplicable(Format.FieldAccess fieldAccess);

  /**
   * Create the inverse behavior graph of a field access function.
   * It also adds the created nodes to {@code vadl.viam.Format.FieldAccess#encoding}.
   */
  void generateEncoding(Parameter parameter, Format.FieldAccess fieldAccess);
}
