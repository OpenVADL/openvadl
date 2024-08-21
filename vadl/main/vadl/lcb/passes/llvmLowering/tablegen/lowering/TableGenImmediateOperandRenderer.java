package vadl.lcb.passes.llvmLowering.tablegen.lowering;


import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateOperand;

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
                    
            def %s
                : %s<%s>
                , ImmLeaf<%s, [{ return %s(Imm); }]>;
            """, operand.rawName(),
        operand.encoderMethod(),
        operand.decoderMethod(),
        operand.fullname(),
        operand.rawName(),
        operand.type().getLlvmType(),
        operand.type().getLlvmType(),
        operand.predicateMethod()
    );
  }
}
