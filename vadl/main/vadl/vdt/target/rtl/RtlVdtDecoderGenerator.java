package vadl.vdt.target.rtl;

import static vadl.vdt.target.rtl.ChiselUtils.toChiselPattern;

import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.types.Type;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
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
    tree.accept(this);
    return appendable.toString();
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

    /* The order is not particularly important, but might be dictated by the set in the decision
     * tree (e.g. linked hash set) */
    final List<Map.Entry<BitPattern, Node>> children = n.getChildren().entrySet().stream().toList();

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

    for (Signal signal : signals) {

      appendable
          .append(signal.simpleName())
          .append(" := ");

      if (isInvalid(signal)) {
        appendable.appendLn("true.B");
      } else {
        appendable.appendLn(getDefaultValue(signal));
      }
    }

    appendable.unindent()
        .appendLn("}");

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
        .append(" // ").appendLn(insnName);

    for (Signal signal : signals) {

      appendable
          .append(signal.simpleName())
          .append(" := ");

      var value = decision == null ? null : decision.get(signal);

      if (value == null) {
        appendable.appendLn(getDefaultValue(signal));
        continue;
      }

      appendable.appendLn(toChiselValue(value.constant()));
    }

    return null;
  }

  private boolean isInvalid(Signal signal) {
    return signal.identifier.equals(invalidInsn.identifier);
  }

  public static CharSequence toChiselValue(Constant constant) {

    if (constant.type().isTrivialCastTo(Type.bool())) {
      return constant.asVal().bool() + ".B";
    }

    if (!constant.type().isDataType()) {
      throw new ViamError("Signal type not supported: %s", constant.type());
    }

    return constant.asVal().unsignedInteger() + ".U";
  }

  public static CharSequence getDefaultValue(Signal signal) {
    if (signal.type().isTrivialCastTo(Type.bool())) {
      return "false.B";
    }

    if (!signal.type().isDataType()) {
      throw new ViamError("Signal type not supported: %s", signal.type());
    }

    return "0.U";
  }

}
