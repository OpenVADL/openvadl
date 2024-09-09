package vadl.lcb.codegen.relocation;

import vadl.lcb.codegen.CodeGenerator;

/**
 * Generates the code for relocations.
 */
public class RelocationCodeGenerator extends CodeGenerator {
  @Override
  public String getFunctionName(String rawName) {
    return rawName;
  }
}
