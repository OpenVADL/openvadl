// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.vdt.target.iss;

import static vadl.error.Diagnostic.error;
import static vadl.utils.MemOrderUtils.reverseByteOrder;
import static vadl.utils.StringBuilderUtils.join;
import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;
import static vadl.vdt.utils.BitVectorUtils.fittingPowerOfTwo;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.common.AccessFunctionCodeGenerator;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticBuilder;
import vadl.error.DiagnosticList;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.impl.regular.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.model.impl.LeafNodeImpl;
import vadl.vdt.target.common.dto.DecisionTreeStatistics;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.viam.Constant;
import vadl.viam.Constant.BitSlice.Part;
import vadl.viam.Format;

/**
 * Generate C/C++ code for a decision tree from an in-memory representation of the decision tree.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"})
public class IssDecisionTreeCodeGenerator implements Visitor<Void> {

  private final CodeGeneratorAppendable appendable = new StringBuilderAppendable();

  private final Node tree;
  private final DecisionTreeStatistics stats;
  private final ByteOrder byteOrder;

  /**
   * Construct the decode tree generator.
   *
   * @param tree      The vadl decode tree
   * @param byteOrder The architecture's memory byte order for extraction of format fields.
   */
  public IssDecisionTreeCodeGenerator(Node tree, ByteOrder byteOrder) {
    this.tree = tree;
    this.byteOrder = byteOrder;
    this.stats = statistics(tree);
  }

  /**
   * Generate the code for the given decision tree.
   */
  public CharSequence generate() {

    // Step 0: Generate code for DTOs (structs) holding the decoded fields (one for each format)

    final List<vadl.viam.Instruction> insns = getInstructions(tree);
    final List<Format> formats = getFormats(insns);

    generateFormatStructs(formats);

    // Step 1: Generate function declarations for the translation functions (for compatibility
    //           with translate.c)
    generateTranslateDeclarations(insns);

    // Step 2: Generate code to extract the fields from the instruction word to the DTOs
    final String insnWordCType = CppTypeMap.getCppTypeNameByVadlType(getInsnWordType());
    generateFormatExtractors(insnWordCType, formats);

    // Step 3: Generate code for the decoding decision tree

    appendable.append("static uint8_t decode_insn(")
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

    appendable.append("return 0;\n");

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
  @Handler
  public Void handle(InnerNodeImpl node) {

    final BigInteger mask = node.getMask().toValue();
    final Map<BitPattern, Node> children = node.getChildren();

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

    if (node.getFallback() != null) {
      appendable.append("default:\n");
      appendable.indent();
      node.getFallback().accept(this);
      appendable.unindent();
    } else {
      appendable.append("default:\n").indent().append("return 0;\n").unindent();
    }

    appendable.unindent();
    appendable.append("}\n");

    return null;
  }

  /**
   * Generate the decision code for a multi-decision (switch) node.
   *
   * @param node The inner decision node.
   * @return Void
   */
  @Handler
  public Void handle(MultiDecisionNode node) {

    final DataType insnType = getInsnWordType();
    final int insnWidth = insnType.bitWidth();

    final BigInteger mask = node.getMask().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();

    final Map<BitPattern, Node> children = node.getChildren();

    int shift = insnWidth - (node.getOffset() + length);
    if (offset > 0 && shift > 0) {
      appendable.append("switch ((insn >> %d) & 0x%s) {\n".formatted(
          shift, mask.toString(16)
      ));
    } else {
      appendable.append("switch (insn & 0x")
          .append(mask.toString(16))
          .append(") {\n");
    }

    appendable.indent();

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      final BigInteger caseValue = entry.getKey().toBitVector().toValue();
      appendable.append("case 0x").append(caseValue.toString(16))
          .append(":\n");

      appendable.indent();
      entry.getValue().accept(this);
      appendable.unindent();
    }

    appendable.append("default:\n")
        .indent()
        .append("return 0;\n")
        .unindent();

    appendable.unindent();
    appendable.append("}\n");

    return null;
  }

  /**
   * Generate the decision code for a single-decision (if/else) node.
   *
   * @param node The inner decision node.
   * @return Void
   */
  @Handler
  public Void handle(SingleDecisionNode node) {

    final DataType insnType = getInsnWordType();
    final int insnWidth = insnType.bitWidth();

    final BigInteger mask = node.getPattern().toMaskVector().toValue();
    final BigInteger value = node.getPattern().toBitVector().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();

    int shift = insnWidth - (node.getOffset() + length);
    if (offset > 0 && shift > 0) {
      appendable.append("if ((insn >> %d) & 0x%x == 0x%x) {\n"
          .formatted(shift, mask, value));
    } else {
      appendable.append("if (insn & 0x%x ==  0x%x) {\n"
          .formatted(mask, value));
    }

    appendable.indent();
    node.getMatchingChild().accept(this);
    appendable.unindent();

    appendable.append("} else {");

    appendable.indent();
    node.getOtherChild().accept(this);
    appendable.unindent();

    appendable.append("}\n");

    return null;
  }

  /**
   * An inner node represents a decision point in the decision tree. We generate a switch statement
   * to select the correct child node based on relevant bits in the instruction word.
   *
   * @param node The inner node
   */
  @Override
  public Void visit(InnerNode node) {
    IssDecisionTreeCodeGeneratorDispatcher.dispatch(this, node);
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

    if (!(node instanceof LeafNodeImpl(Instruction insn))) {
      throw new IllegalArgumentException("Leaf node type not supported: " + node.getClass());
    }

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
        .append(") ? ")
        .append(insn.source().format().type().bitWidth() / 8)
        .appendLn(" : 0;");

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

    final int insnWidth = getInsnWordType().bitWidth();
    final int formatWidth = field.format().type().bitWidth();
    final int shift = insnWidth - formatWidth;

    // Shift the bit slices according to the insn word width
    var parts = field.bitSlice().parts()
        .map(p -> Part.of(p.msb() + shift, p.lsb() + shift))
        .toList();
    var slice = new Constant.BitSlice(parts.toArray(new Part[0]));

    // Possibly translate to little-endian format
    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
      slice = reverseByteOrder(slice, insnWidth);
      parts = slice.parts().toList();
    }

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

      if (lsb == 0) {
        expr.append("(insn");
      } else {
        expr.append("((insn >> ").append(lsb).append(")");
      }

      expr.append(" & 0x")
          .append(BigInteger.valueOf((1L << width) - 1).toString(16))
          .append(")");

      if (offset > 0) {
        expr.append(" << ").append(offset).append(")");
      }

      offset += width;

      // Insert at the front of the list to maintain the correct order (msb to lsb)
      partExpressions.addFirst(expr);
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
    var fieldRefs = access.fieldRefs().stream()
        .collect(Collectors.toMap(Function.identity(), f -> "a->" + f.simpleName()));
    var generator = new AccessFunctionCodeGenerator(access, null, fieldRefs);
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
   * @return the C++ type of the instruction word
   */
  private DataType getInsnWordType() {
    var maxWidth = stats.getMaxInstructionWidth();
    int bitWidth = fittingPowerOfTwo(maxWidth);

    var resultType = BitsType.bits(bitWidth).fittingCppType();

    if (resultType == null) {
      // For every instruction format > 128 bit, throw a diagnostic. In the future the ISS decoder
      // may be adapted to handle arbitrary instruction widths.
      final List<Diagnostic> diagnostics = getFormats(getInstructions(tree))
          .stream()
          .filter(f -> f.type().bitWidth() > 128)
          .map(f ->
              error("Instructions of more than 128 bit are currently not supported by the "
                  + "decoder generator.", f)
                  .help("Reduce the width of the instruction format."))
          .map(DiagnosticBuilder::build)
          .toList();
      throw new DiagnosticList(diagnostics);
    }

    return resultType;
  }

}
