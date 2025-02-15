package vadl.vdt.target.hw;

import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import vadl.vdt.impl.theiling.InnerNodeImpl;
import vadl.vdt.impl.theiling.LeafNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.PBit;
import vadl.vdt.utils.codegen.CodeGeneratorAppendable;
import vadl.vdt.utils.codegen.StringBuilderAppendable;

/**
 * Generate a self-contained Chisel module for decoding instructions to control signals used for
 * hardware generation.
 */
public class HardwareIrregularDecoderGenerator implements Visitor<Void> {

  private final CodeGeneratorAppendable appendable = new StringBuilderAppendable();
  private final Map<String, Integer> instructions = new HashMap<>();

  /**
   * Generate the decoder module for the given decision tree.
   *
   * @param tree The decode decision tree to generate code for
   */
  // TODO: this API will have to be extended to map the instructions to their control signals
  public CharSequence generate(Node tree) {

    // TODO: Remove build info
    appendable
        .appendLn("""
            //> using scala "2.13.12"
            //> using dep "org.chipsalliance::chisel:6.6.0"
            //> using plugin "org.chipsalliance:::chisel-plugin:6.6.0"
            //> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"
            """)
        .newLine()
        .appendLn("import _root_.circt.stage.ChiselStage")
        .newLine();

    // Imports (could be moved to the template)
    appendable
        .appendLn("import chisel3._")
        .appendLn("import chisel3.util.BitPat")
        .newLine();

    // Step 1: Generate the module I/O interface

    appendable
        .appendLn("class InstructionDecoder extends Module {")
        .indent();

    var stats = statistics(tree);

    int insnWordWidth = stats.getMaxInstructionWidth();
    appendable
        .append("val input = IO(Input(UInt(").append(insnWordWidth).append(".W)))")
        .newLine();

    // TODO: for now just enumerate the instructions. Later we'll emit actual control signals
    var insns = getInstructions(tree);
    IntStream.range(0, insns.size())
        // Make sure idx 0 is not used, as it will indicate an invalid instruction
        .forEach(i -> instructions.put(insns.get(i), i + 1));

    // + 1 for the default value '0'
    int signalsWidth = BigInteger.valueOf(stats.getNumberOfLeafNodes() + 1).bitLength();
    appendable
        .append("val output = IO(Output(UInt(").append(signalsWidth).append(".W)))")
        .newLine().newLine();

    // Generate the actual decision logic
    tree.accept(this);

    appendable.unindent().append("}");

    // TODO: remove APP definition
    appendable.newLine()
        .appendLn("""
            object Main extends App {
              println(
                ChiselStage.emitSystemVerilog(
                  gen = new InstructionDecoder,
                  firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
                )
              )
            }
            """)
        .newLine();

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

    // The order is not particularly important, but might be dictated by the set in the decision
    // tree (e.g. linked hash set);
    final List<Map.Entry<BitPattern, Node>> children = n.getChildren().entrySet().stream().toList();

    for (int i = 0; i < children.size(); i++) {
      var entry = children.get(i);

      // Construct the condition
      appendable
          .append(i == 0 ? "when" : ".elsewhen")
          .append(" (")
          .append("input === BitPat(\"").append(toChiselPattern(entry.getKey())).append("\")")
          .appendLn(") {")
          .indent();

      entry.getValue().accept(this);

      appendable
          .unindent()
          .append("}");
    }

    // TODO: Think about a default branch which sets an (in-)valid flag, as this indicates an error.
    appendable
        .appendLn(".otherwise {")
        .indent()
        // For now, we choose 0 to indicate an invalid instruction
        .append("output := 0.U").appendLn(" // invalid")
        .unindent()
        .appendLn("}");

    return null;
  }

  /**
   * A leaf node represents an instruction.
   *
   * @param node The leaf node
   */
  @Override
  public Void visit(LeafNode node) {

    if (!(node instanceof LeafNodeImpl lf)) {
      throw new IllegalArgumentException("Leaf node type not supported: " + node.getClass());
    }

    var key = lf.instruction().source().simpleName();
    var idx = instructions.get(key);

    if (idx == null) {
      throw new IllegalStateException("Unable to find index by instruction " + key);
    }

    appendable
        .append("output := ").append(idx).append(".U")
        .append(" // ").append(key)
        .newLine();

    return null;
  }

  /**
   * Get the list of instructions in the decision tree.
   *
   * @param tree the decision tree
   * @return the list of instructions
   */
  private List<String> getInstructions(Node tree) {
    if (tree instanceof LeafNode lf) {
      return List.of(lf.instruction().source().simpleName());
    } else if (tree instanceof InnerNode in) {
      return in.children().stream()
          .map(this::getInstructions)
          .flatMap(List::stream)
          .toList();
    } else {
      throw new IllegalArgumentException("Unsupported node type: " + tree.getClass());
    }
  }

  private CharSequence toChiselPattern(BitPattern pattern) {
    final var sb = new StringBuilder("b");

    boolean isLeadingWildcard = true;
    for (int i = 0; i < pattern.width(); i++) {
      if (pattern.get(i).getValue() == PBit.Value.DONT_CARE && isLeadingWildcard) {
        continue;
      }
      isLeadingWildcard = false;
      sb.append(pattern.get(i).getValue() == PBit.Value.ONE ? '1' : (
          pattern.get(i).getValue() == PBit.Value.ZERO ? '0' : '?'));
    }
    return sb;
  }
}
