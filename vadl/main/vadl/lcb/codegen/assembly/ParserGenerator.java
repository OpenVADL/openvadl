package vadl.lcb.codegen.assembly;

import java.util.List;
import java.util.stream.Collectors;
import vadl.gcb.passes.assemblyConstantIntern.AssemblyConstant;
import vadl.gcb.passes.assemblyConstantIntern.AssemblyReplacementConstantPass;
import vadl.viam.Format;
import vadl.viam.Instruction;

/**
 * A helper struct for generating parser code in LLVM.
 */
public class ParserGenerator {
  /**
   * Generates a struct name based on a list of {@link Format.Field}.
   * A format with {@code rd}, {@code mnemonic} and {@code rs1} should
   * generated {@code rdmnemonicrs1}.
   */
  public static String generateStructName(List<Format.Field> fields) {
    return fields.stream()
        .map(ParserGenerator::generateFieldName)
        .collect(Collectors.joining());
  }

  /**
   * Generate a name for the function given a {@link AssemblyConstant}.
   */
  public static String generateConstantName(
      AssemblyConstant assemblyConstant) {
    return assemblyConstant.kind().name().toUpperCase();
  }

  /**
   * Generate a name for the function given a {@link Instruction}.
   */
  public static String generateInstructionName(Instruction instruction) {
    return instruction.identifier.simpleName().trim() + "Instruction";
  }

  /**
   * Generate a field name from a {@link Format.Field}.
   */
  public static String generateFieldName(Format.Field field) {
    return field.identifier.simpleName().trim();
  }
}
