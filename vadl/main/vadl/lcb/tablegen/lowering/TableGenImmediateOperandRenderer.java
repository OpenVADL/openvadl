package vadl.lcb.tablegen.lowering;


import vadl.lcb.tablegen.model.TableGenImmediateOperand;

/**
 * Utility class for mapping into tablegen.
 */
public final class TableGenImmediateOperandRenderer {

  /**
   * Transforms the given {@code operand} into a string which can be used by LLVM's TableGen.
   */
  public static String lower(TableGenImmediateOperand operand) {
    return String.format("""
        
        class %s<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "%s";
          let DecoderMethod = "%s";
        }
        """, operand.getName(), operand.getEncoderMethod(), operand.getDecoderMethod());
  }
}
