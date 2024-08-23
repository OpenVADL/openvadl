package vadl.lcb.passes.llvmLowering;

/**
 * This is a marker trait that the side effect is also part of the pattern.
 * The normal patterns don't require any output because they are already
 * defined in the instruction.
 * {@code def : Pat<(add X:$rs1, X:$rs2),
 * (ADD X:$rs1, X:$rs2)>;}
 * However, stores require an output like {@code trunstorei8}.
 * {@code def : Pat<(truncstorei8 X:$rs2, (add X:$rs1,
 * RV32I_Stype_ImmediateS_immediateAsInt32:$imm)),
 * (SB X:$rs1, X:$rs2, RV32I_Stype_ImmediateS_immediateAsInt32:$imm)>;}
 * This interfaces defines that {@code truncstorei8} is also part of the pattern and added to
 * the pattern graph.
 */
public interface LlvmSideEffectPatternIncluded {
}
