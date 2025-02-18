package vadl.lcb.passes.llvmLowering.tablegen.lowering;


import vadl.gcb.passes.GenerateValueRangeImmediatePass;
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
        GenerateValueRangeImmediatePass.highestPossibleValue(operand.formatFieldBitSize(), rawType);
    int lowestPossibleValue =
        GenerateValueRangeImmediatePass.lowestPossibleValue(operand.formatFieldBitSize(), rawType);
    return String.format("""
            class %s<ValueType ty> : Operand<ty>
            {
              let EncoderMethod = "%s";
              let DecoderMethod = "%s";
            }
                    
            def %s
                : %s<%s>
                , ImmLeaf<%s, [{ return Imm >= %s && Imm <= %s && %s(Imm); }]>;
                
            def %sAsLabel : %s<OtherVT>;
            """, operand.rawName(),
        operand.encoderMethod(),
        operand.decoderMethod(),
        operand.fullname(),
        operand.rawName(),
        operand.llvmType().getLlvmType(),
        operand.llvmType().getLlvmType(),
        lowestPossibleValue,
        highestPossibleValue,
        operand.predicateMethod(),
        operand.rawName(),
        operand.rawName()
    );
  }
}
