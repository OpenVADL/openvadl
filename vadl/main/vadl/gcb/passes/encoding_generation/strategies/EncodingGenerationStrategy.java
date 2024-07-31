package vadl.gcb.passes.encoding_generation.strategies;

import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
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
  void generateEncoding(Format.FieldAccess fieldAccess);

  /**
   * Creates a new function for {@link Encoding}. This function has side effects for the
   * {@code fieldAccess}.
   *
   * @param fieldAccess for which the encoding should be generated.
   * @return the {@link Parameter} which is the input for the encoding function.
   */
  default Parameter setupEncodingForFieldAccess(Format.FieldAccess fieldAccess) {
    var ident = fieldAccess.identifier.append("encoding");
    var identParam = ident.append(fieldAccess.name());
    var param = new Parameter(identParam, fieldAccess.accessFunction().returnType());
    var function =
        new Function(ident, new Parameter[] {param}, fieldAccess.fieldRef().type());

    fieldAccess.setEncoding(function);
    return param;
  }
}
