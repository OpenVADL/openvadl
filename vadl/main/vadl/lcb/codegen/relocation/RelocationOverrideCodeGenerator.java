package vadl.lcb.codegen.relocation;

import vadl.lcb.codegen.CodeGenerator;
import vadl.viam.Format;

/**
 * Generates functions which override the fields in {@link Format}.
 */
public class RelocationOverrideCodeGenerator extends CodeGenerator {
  @Override
  protected String getFunctionName(String rawName) {
    return rawName;
  }
}
