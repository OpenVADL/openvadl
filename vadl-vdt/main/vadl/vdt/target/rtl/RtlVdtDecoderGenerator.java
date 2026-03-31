// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.vdt.target.rtl;

import static vadl.vdt.target.rtl.ChiselUtils.toChiselPattern;

import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.Type;
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
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.viam.Constant;
import vadl.viam.Signal;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Generate the decision tree for decoding control signals from the instruction word used within
 * the RTL description.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"})
public class RtlVdtDecoderGenerator implements Visitor<Void> {

  private final CodeGeneratorAppendable appendable = new StringBuilderAppendable();

  /**
   * The name reference of the instruction word.
   */
  private final String input;

  /**
   * The decision to make if an instruction matches.
   */
  private final Map<vadl.viam.Instruction, Map<Signal, ConstantNode>> decisionMap;

  /**
   * The available signals to assign.
   */
  private final Set<Signal> signals;

  /**
   * The signal (flag) representing the invalid instruction.
   */
  private final Signal invalidInsn;

  /**
   * Construct the RtlVdtDecoderGenerator.
   *
   * @param input       The name of the instruction word input variable.
   * @param decisionMap The decisions by instruction.
   * @param signals     The set of signals decided by the decoder.
   * @param invalidInsn The signal to set in case of an invalid instruction.
   */
  public RtlVdtDecoderGenerator(
      String input,
      Map<vadl.viam.Instruction, Map<Signal, ConstantNode>> decisionMap,
      Set<Signal> signals,
      Signal invalidInsn) {
    this.input = input;
    this.decisionMap = decisionMap;
    this.signals = signals;
    this.invalidInsn = invalidInsn;
  }

  /**
   * Generate the decode decision tree for the RTL description.
   *
   * @param tree The decode decision tree to generate code for
   */
  public String generate(Node tree) {
    // The template engine only indents the first line, so to format everything nicely, add an extra
    // indent level, and remove the initial indent at the end.
    appendable.indent();

    // Initialize signals with default values
    for (Signal signal : signals) {

      appendable
          .append(signal.simpleName())
          .append(" := ")
          .appendLn(getDefaultValue(signal));
    }

    appendable.newLine();

    tree.accept(this);
    appendable.unindent();
    return appendable.toString().stripLeading();
  }

  /**
   * An inner node represents a decision point in the decision tree.
   *
   * @param node The regular inner node
   */
  @Handler
  public Void handle(InnerNodeImpl node) {

    /* The order is not particularly important, but might be dictated by the set in the decision
     * tree (e.g. linked hash set) */
    final List<Map.Entry<BitPattern, Node>> children =
        node.getChildren().entrySet().stream().toList();

    for (int i = 0; i < children.size(); i++) {
      var entry = children.get(i);

      // Construct the condition
      appendable
          .append(i == 0 ? "when" : ".elsewhen")
          .append(" (")
          .append(input)
          .append(" === BitPat(\"").append(toChiselPattern(entry.getKey(), false)).append("\")")
          .appendLn(") {")
          .indent();

      entry.getValue().accept(this);

      appendable
          .unindent()
          .append("}");
    }

    appendable
        .appendLn(".otherwise {")
        .indent();

    if (node.getFallback() != null) {

      node.getFallback().accept(this);

    } else {

      appendable.appendLn("// Invalid");

      appendable
          .append(invalidInsn.simpleName())
          .appendLn(" := true.B");

    }

    appendable.unindent()
        .appendLn("}");

    return null;
  }

  /**
   * An inner node representing a switch-style decision.
   *
   * @param node The inner node
   */
  @Handler
  public Void handle(MultiDecisionNode node) {

    /* The order is not particularly important, but might be dictated by the set in the decision
     * tree (e.g. linked hash set) */
    final List<Map.Entry<BitPattern, Node>> children =
        node.getChildren().entrySet().stream().toList();

    for (int i = 0; i < children.size(); i++) {
      var entry = children.get(i);

      // Construct the condition
      appendable
          .append(i == 0 ? "when" : ".elsewhen")
          .append(" (")
          .append(input)
          .append(" === BitPat(\"").append(toChiselPattern(entry.getKey(), false)).append("\")")
          .appendLn(") {")
          .indent();

      entry.getValue().accept(this);

      appendable
          .unindent()
          .append("}");
    }

    appendable
        .appendLn(".otherwise {")
        .indent()
        .appendLn("// Invalid");

    appendable
        .append(invalidInsn.simpleName())
        .appendLn(" := true.B");

    appendable.unindent()
        .appendLn("}");

    return null;
  }

  /**
   * An inner node representing an either-or decision.
   *
   * @param node The inner node
   */
  @Handler
  public Void handle(SingleDecisionNode node) {

    // Emit the condition
    appendable
        .append("when (")
        .append(input)
        .append(node.isMatch() ? " === " : " !== ")
        .append("BitPat(\"").append(toChiselPattern(node.getPattern(), false)).append("\")")
        .appendLn(") {")
        .indent();

    node.getMatchingChild().accept(this);

    appendable
        .unindent()
        .append("}");

    // Emit the else branch

    appendable
        .appendLn(".otherwise {")
        .indent();

    if (node.getOtherChild() != null) {
      node.getOtherChild().accept(this);
    } else {
      // If we don't have an 'other' option, fall back to 'invalid'
      appendable
          .appendLn("// Invalid")
          .append(invalidInsn.simpleName())
          .appendLn(" := true.B");
    }

    appendable.unindent()
        .appendLn("}");

    return null;
  }

  /**
   * An inner node represents a decision point in the decision tree. Dispatch for the different
   * types of decision nodes.
   *
   * @param node The inner node
   */
  @Override
  public Void visit(InnerNode node) {
    RtlVdtDecoderGeneratorDispatcher.dispatch(this, node);
    return null;
  }

  /**
   * A leaf node represents a successfully matched instruction.
   *
   * @param node The leaf node
   */
  @Override
  public Void visit(LeafNode node) {

    if (!(node instanceof LeafNodeImpl(Instruction instruction))) {
      throw new IllegalArgumentException("Leaf node type not supported: " + node.getClass());
    }

    var insnName = instruction.source().simpleName();
    var decision = decisionMap.get(instruction.source());

    appendable
        .append("// ").appendLn(insnName);

    for (Signal signal : signals) {

      var value = decision == null ? null : decision.get(signal);

      if (value == null) {
        continue;
      }

      appendable
          .append(signal.simpleName())
          .append(" := ")
          .appendLn(toChiselValue(value.constant()));
    }

    return null;
  }

  private static CharSequence toChiselValue(Constant constant) {

    if (constant.type().isTrivialCastTo(Type.bool())) {
      return constant.asVal().bool() + ".B";
    }

    if (!constant.type().isDataType()) {
      throw new ViamError("Signal type not supported: %s", constant.type());
    }

    return constant.asVal().unsignedInteger() + ".U";
  }

  private static CharSequence getDefaultValue(Signal signal) {
    if (signal.type().isTrivialCastTo(Type.bool())) {
      return "false.B";
    }

    if (!signal.type().isDataType()) {
      throw new ViamError("Signal type not supported: %s", signal.type());
    }

    return "0.U";
  }

}
