package vadl.vdt.target.iss;

import static vadl.utils.StringBuilderUtils.join;
import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.common.AccessFunctionCodeGenerator;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.vdt.impl.theiling.InnerNodeImpl;
import vadl.vdt.impl.theiling.LeafNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.codegen.CodeGeneratorAppendable;
import vadl.vdt.utils.codegen.StringBuilderAppendable;
import vadl.viam.Format;

/**
 * Generate C/C++ code for a decision tree from an in-memory representation of the decision tree.
 */
public class IssDecisionTreeCodeGenerator implements Visitor<Void> {

  private final CodeGeneratorAppendable appendable = new StringBuilderAppendable();

  /**
   * Generate the code for the given decision tree.
   *
   * @param tree The decode decision tree to generate code for
   */
  public CharSequence generate(Node tree) {

    // Imports (could be moved to the template)
    appendable
        .append("#include <stdint.h>\n")
        .append("#include \"vadl-builtins.h\"\n\n");

    // TODO: Potentially pass the ISA here, so we don't have to traverse the tree for the format
    //       and instruction definitions.

    // Step 0: Generate code for DTOs (structs) holding the decoded fields (one for each format)

    final List<vadl.viam.Instruction> insns = getInstructions(tree);
    final List<Format> formats = getFormats(insns);

    generateFormatStructs(formats);

    // Step 1: Generate function declarations for the translation functions (for compatibility
    //           with translate.c)
    generateTranslateDeclarations(insns);

    // Step 2: Generate code to extract the fields from the instruction word to the DTOs

    final String insnWordCType = CppTypeMap.getCppTypeNameByVadlType(getInsnWordType(tree));
    generateFormatExtractors(insnWordCType, formats);

    // Step 3: Generate code for the decoding decision tree

    appendable.append("static bool decode_insn(")
        .append("DisasContext *ctx, ")
        .append(insnWordCType).append(" insn) {\n\n");

    appendable.indent();

    // Prepare the union of all instruction argument structs
    appendable.append("union {\n").indent();

    for (var format : formats) {
      appendable
          .append("arg_")
          .append(format.simpleName().toLowerCase(Locale.US)).append(" ")
          .append(format.simpleName().toLowerCase()).append(";\n");
    }

    appendable.unindent();
    appendable.append("} insn_args;\n\n");

    // Generate the actual decision tree code
    tree.accept(this);

    appendable.append("return false;\n");

    appendable.unindent();
    appendable.append("}\n");

    return appendable.toCharSequence();
  }

  /**
   * An inner node represents a decision point in the decision tree. We generate a switch statement
   * to select the correct child node based on relevant bits in the instruction word.
   *
   * @param node The inner node
   */
  @Override
  public Void visit(InnerNode node) {

    if (!(node instanceof InnerNodeImpl n)) {
      throw new IllegalArgumentException("Node type not supported: " + node.getClass());
    }

    final BigInteger mask = n.getMask().toValue();
    final Map<BitPattern, Node> children = n.getChildren();

    appendable.append("switch (insn & 0x")
        .append(mask.toString(16))
        .append(") {\n");

    appendable.indent();

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      final BigInteger caseValue = entry.getKey().toBitVector().toValue();
      appendable.append("case 0x").append(caseValue.toString(16))
          .append(":\n");

      appendable.indent();
      entry.getValue().accept(this);
      appendable.unindent();
    }

    if (n.getFallback() != null) {
      appendable.append("default:\n");
      appendable.indent();
      n.getFallback().accept(this);
      appendable.unindent();
    } else {
      appendable.append("default:\n").indent().append("return false;\n").unindent();
    }

    appendable.unindent();
    appendable.append("}\n");

    return null;
  }

  /**
   * A leaf node represents an instruction. We generate the code to extract the fields from the
   * instruction word and call the translation function for the instruction.
   *
   * @param node The leaf node
   */
  @Override
  public Void visit(LeafNode node) {

    if (!(node instanceof LeafNodeImpl lf)) {
      throw new IllegalArgumentException("Leaf node type not supported: " + node.getClass());
    }

    final Instruction insn = lf.instruction();

    // Extract the fields from the instruction word
    appendable.append("extract_")
        .append(insn.source().format().simpleName().toLowerCase(Locale.US))
        .append("(insn, &insn_args.")
        .append(insn.source().format().simpleName().toLowerCase(Locale.US))
        .append(");\n");
    // Call the translation function
    appendable.append("return trans_")
        .append(insn.source().simpleName().toLowerCase(Locale.US))
        .append("(ctx, &insn_args.")
        .append(insn.source().format().simpleName().toLowerCase(Locale.US))
        .append(");\n");

    return null;
  }

  /**
   * Generate the function declarations for the translation functions. For compatibility with the
   * existing translate.c file we generate a type alias for the instruction argument struct.
   *
   * @param insns The set of instructions
   */
  private void generateTranslateDeclarations(List<vadl.viam.Instruction> insns) {
    for (var insn : insns) {
      var insnName = insn.simpleName().toLowerCase(Locale.US);
      // Generate type alias for the instruction argument struct
      appendable.append("typedef arg_")
          .append(insn.format().simpleName().toLowerCase(Locale.US))
          .append(" arg_")
          .append(insnName)
          .append(";\n");
      // Generate translation function declaration
      appendable.append("static bool trans_")
          .append(insnName)
          .append("(DisasContext *ctx, arg_")
          .append(insnName)
          .append(" *insn);\n");
    }
    appendable.newLine();
  }

  /**
   * Generate the structs for holding the decoded fields of the instructions.
   *
   * @param formats The set of different formats of the instructions
   */
  private void generateFormatStructs(List<Format> formats) {
    for (Format format : formats) {
      appendable.append("typedef struct {\n");
      appendable.indent();

      // Persistent fields
      for (var field : format.fields()) {
        var fieldName = field.simpleName();
        var fieldType = field.type().fittingCppType();
        if (fieldType == null) {
          throw new IllegalArgumentException("Unsupported field type: " + field.type());
        }
        var typeStr = CppTypeMap.getCppTypeNameByVadlType(fieldType);
        appendable
            .append(typeStr).append(" ").append(fieldName)
            .append(";\n");
      }

      // Pseudo fields
      for (var access : format.fieldAccesses()) {
        var fieldName = access.simpleName();
        var fieldType = access.type().asDataType().fittingCppType();
        if (fieldType == null) {
          throw new IllegalArgumentException("Unsupported field type: " + access.type());
        }
        var typeStr = CppTypeMap.getCppTypeNameByVadlType(fieldType);
        appendable
            .append(typeStr).append(" ").append(fieldName)
            .append(";\n");
      }

      appendable.unindent();

      var formatName = format.simpleName().toLowerCase();
      appendable.append("} arg_").append(formatName).append(";\n\n");
    }
  }

  /**
   * Generate the code for extracting the fields from the instruction word to the format DTOs.
   *
   * @param insnWordCType The C type of the instruction word
   * @param formats       The set of different formats of the instructions
   */
  private void generateFormatExtractors(String insnWordCType, List<Format> formats) {
    for (Format format : formats) {

      var formatName = format.simpleName().toLowerCase();
      appendable.append("static void extract_").append(formatName)
          .append("(").append(insnWordCType).append(" insn, arg_").append(formatName)
          .append(" *a) {\n");

      appendable.indent();

      // Persistent fields
      for (var field : format.fields()) {
        var fieldName = field.simpleName();

        appendable
            .append("a->").append(fieldName).append(" = ")
            .append(extractField(field))
            .append(";\n");
      }

      // Pseudo fields
      for (var access : format.fieldAccesses()) {
        var fieldName = access.simpleName();

        appendable
            .append("a->").append(fieldName).append(" = ")
            .append(accessField(access))
            .append(";\n");
      }

      appendable.unindent();
      appendable.append("};\n\n");
    }
  }

  private CharSequence extractField(Format.Field field) {
    var parts = field.bitSlice().parts()
        .toList();

    // Walk from the least significant part to the most significant part. This way we can calculate
    // the offset to shift the extracted value to the correct position in the field.

    final List<CharSequence> partExpressions = new ArrayList<>();

    int offset = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      final var expr = new StringBuilder();

      var part = parts.get(i);

      int lsb = part.lsb();
      int width = part.size();

      if (offset > 0) {
        expr.append("(");
      }

      expr.append("((insn >> ").append(lsb).append(") & 0x")
          .append(BigInteger.valueOf((1L << width) - 1).toString(16)).append(")");

      if (offset > 0) {
        expr.append(" << ").append(offset).append(")");
      }

      offset += width;

      // Insert at the front of the list to maintain the correct order (msb to lsb)
      partExpressions.add(0, expr);
    }

    return join(" | ", partExpressions);
  }

  /**
   * Generate the code for a field access.
   *
   * @param access The field access
   * @return The C++ expression for the field access
   */
  private String accessField(Format.FieldAccess access) {
    var fieldRef = access.fieldRef();
    var refName = "a->" + fieldRef.simpleName();

    var generator = new AccessFunctionCodeGenerator(access, null, refName);
    return generator.genReturnExpression();
  }

  /**
   * Get the list of instructions in the decision tree.
   *
   * @param tree the decision tree
   * @return the list of instructions
   */
  private List<vadl.viam.Instruction> getInstructions(Node tree) {
    if (tree instanceof LeafNode lf) {
      return List.of(lf.instruction().source());
    } else if (tree instanceof InnerNode in) {
      return in.children().stream()
          .map(this::getInstructions)
          .flatMap(List::stream)
          .toList();
    } else {
      throw new IllegalArgumentException("Unsupported node type: " + tree.getClass());
    }
  }

  /**
   * Get the set of different formats of the instructions.
   *
   * @param insns the instructions
   * @return the list of distinct formats
   */
  private List<Format> getFormats(Collection<vadl.viam.Instruction> insns) {
    return insns.stream()
        .map(vadl.viam.Instruction::format)
        .distinct()
        .toList();
  }

  /**
   * Determine the C++ type of the instruction word. We'll use the smallest unsigned integer type
   * that can hold the maximum instruction width.
   *
   * @param tree the decision tree
   * @return the C++ type of the instruction word
   */
  private DataType getInsnWordType(Node tree) {
    var maxWidth = statistics(tree).getMaxInstructionWidth();
    var insnType = BitsType.bits(fittingPowerOfTwo(maxWidth)).fittingCppType();
    if (insnType == null) {
      throw new IllegalArgumentException(
          "Instruction word too wide: " + maxWidth + " bits");
    }
    return insnType;
  }

  /**
   * Find the smallest power of two that is greater or equal to n.
   *
   * @param n the input number
   * @return the smallest fitting power of two
   */
  private int fittingPowerOfTwo(int n) {
    final BigInteger bigN = BigInteger.valueOf(n);
    if (bigN.compareTo(BigInteger.ZERO) <= 0) {
      throw new IllegalArgumentException("Input must be a positive integer");
    }
    if (bigN.getLowestSetBit() == bigN.bitLength() - 1) {
      // n is already a power of two
      return n;
    }
    return BigInteger.ONE.shiftLeft(bigN.bitLength()).intValue();
  }
}
