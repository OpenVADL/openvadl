package vadl.lcb.codegen.relocation;

import vadl.lcb.codegen.CodeGenerator;

/**
 * Generates the code for relocations.
 */
public class RelocationCodeGenerator extends CodeGenerator {
  @Override
  protected String getFunctionName(String rawName) {
    return rawName;
  }
}
