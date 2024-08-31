package vadl.lcb.codegen.assembly;

import static vadl.viam.ViamError.ensure;

import java.util.List;
import java.util.stream.Collectors;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.gcb.passes.assembly.AssemblyRegisterNode;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

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

  /**
   * The recursive descent parser has a {@code struct} for every assembly operand combination.
   */
  public record FieldStructEnumeration(String structName, List<String> fieldNames) {
  }

  /**
   * Generate the assembly operand's names given a {@link BuiltInCall} and the {@link BuiltIn}
   * is {@link BuiltInTable#CONCATENATE_STRINGS}.
   */
  public static FieldStructEnumeration mapParserRecord(
      BuiltInCall builtin) {
    ensure(builtin.builtIn() == BuiltInTable.CONCATENATE_STRINGS, "node must be concat");
    var names = builtin.arguments().stream()
        .map(ParserGenerator::mapToName)
        .filter(x -> !x.isEmpty())
        .toList();

    return new FieldStructEnumeration(String.join("", names), names);
  }

  /**
   * Get the operand name based on the node.
   */
  public static String mapToName(ExpressionNode x) {
    if (x instanceof BuiltInCall b && b.builtIn() == BuiltInTable.MNEMONIC) {
      return "mnemonic";
    } else if (x instanceof BuiltInCall b && b.builtIn() == BuiltInTable.DECIMAL) {
      if (b.arguments().get(0) instanceof FieldRefNode frn) {
        return mapToName(frn);
      } else if (b.arguments().get(0) instanceof FieldAccessRefNode fac) {
        return mapToName(fac);
      }
    } else if (x instanceof BuiltInCall b && b.builtIn() == BuiltInTable.HEX) {
      if (b.arguments().get(0) instanceof FieldRefNode frn) {
        return mapToName(frn);
      } else if (b.arguments().get(0) instanceof FieldAccessRefNode fac) {
        return mapToName(fac);
      }
    } else if (x instanceof AssemblyRegisterNode ar) {
      return ar.field().formatField().identifier.simpleName();
    } else if (x instanceof AssemblyConstant) {
      return "";
    }

    throw new ViamError("not supported");
  }

  private static String mapToName(FieldRefNode node) {
    return node.formatField().identifier.simpleName();
  }

  private static String mapToName(FieldAccessRefNode node) {
    return node.fieldAccess().identifier.simpleName();
  }
}
