package vadl.lcb.passes.llvmLowering.tablegen.lowering;


import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;

/**
 * Utility class for mapping into tablegen.
 */
public final class TableGenImmediateOperandRenderer {

  /**
   * Transforms the given {@code operand} into a string which can be used by LLVM's TableGen.
   */
  public static String lower(TableGenImmediateRecord operand) {
    var type = operand.type().isSigned() ? operand.type() : operand.type().makeSigned();
    return String.format("""
            class %s<ValueType ty> : Operand<ty>
            {
              let EncoderMethod = "%s";
              let DecoderMethod = "%s";
            }
                    
            def %s
                : %s<%s>
                , ImmLeaf<%s, [{ return isInt<%s>(Imm) && %s(Imm); }]>;
                
            def %sAsLabel : %s<OtherVT>;
            """, operand.rawName(),
        operand.encoderMethod(),
        operand.decoderMethod(),
        operand.fullname(),
        operand.rawName(),
        type.getLlvmType(),
        type.getLlvmType(),
        operand.formatFieldBitSize(),
        operand.predicateMethod(),
        operand.rawName(),
        operand.rawName()
    );
  }
}
