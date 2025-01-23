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
    var rawType = operand.rawType();
    int highestPossibleValue =
        (int) (rawType.isSigned()
            ? Math.pow(2, (double) operand.formatFieldBitSize() - 1)
            : Math.pow(2, operand.formatFieldBitSize())) - 1;
    int lowestPossibleValue =
        rawType.isSigned()
            ? (int) (-1 * Math.pow(2, (double) operand.formatFieldBitSize() - 1))
            : 0;
    return String.format("""
            class %s<ValueType ty> : Operand<ty>
            {
              let EncoderMethod = "%s";
              let DecoderMethod = "%s";
            }
                    
            def %s
                : %s<%s>
                , ImmLeaf<%s, [{ return isInt<%s>(Imm) && Imm >= %s && Imm <= %s && %s(Imm); }]>;
                
            def %sAsLabel : %s<OtherVT>;
            """, operand.rawName(),
        operand.encoderMethod(),
        operand.decoderMethod(),
        operand.fullname(),
        operand.rawName(),
        operand.llvmType().getLlvmType(),
        operand.llvmType().getLlvmType(),
        operand.formatFieldBitSize(),
        lowestPossibleValue,
        highestPossibleValue,
        operand.predicateMethod(),
        operand.rawName(),
        operand.rawName()
    );
  }
}
