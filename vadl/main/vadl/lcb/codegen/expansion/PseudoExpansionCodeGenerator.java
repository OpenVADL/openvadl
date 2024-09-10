package vadl.lcb.codegen.expansion;

import vadl.lcb.codegen.CodeGenerator;
import vadl.viam.PseudoInstruction;

/**
 * Generates functions which expands {@link PseudoInstruction} in LLVM.
 */
public class PseudoExpansionCodeGenerator extends CodeGenerator {
  @Override
  public String getFunctionName(String rawName) {
    return rawName + "_expand";
  }
}
