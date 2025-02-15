package vadl.vdt.target.hw;

import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;

import java.math.BigInteger;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;
import vadl.vdt.utils.codegen.CodeGeneratorAppendable;
import vadl.vdt.utils.codegen.StringBuilderAppendable;

/**
 * Generate a Chisel module for decoding fixed-length non-overlapping instructions to control
 * signals used for hardware generation.
 * <br> This uses Chisel's decoder library and built-in circuit optimization (qmc, espresso).
 */
public class HardwareRegularDecoderGenerator {

  private final CodeGeneratorAppendable appendable = new StringBuilderAppendable();

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
        .appendLn("import chisel3.util.experimental.decode._")
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

    var insns = getInstructions(tree);

    // TODO: Emit actual control signals
    int signalsWidth = BigInteger.valueOf(insns.size() + 1).bitLength();
    appendable
        .append("val output = IO(Output(UInt(").append(signalsWidth).append(".W)))")
        .newLine().newLine();

    // Construct Chisel's decoder table
    appendable
        .appendLn("val table = TruthTable(")
        .indent()
        .appendLn("Map(")
        .indent();

    for (int i = 0; i < insns.size(); i++) {
      var insn = insns.get(i);
      appendable
          .append("BitPat(\"").append(toChiselPattern(insn.pattern())).append("\")")
          .append(" -> ")
          // Make sure the output value doesn't collide with 0, which indicates an invalid insn
          .append("BitPat(\"").append(toChiselPattern(i + 1, signalsWidth)).append("\")")
          .appendLn(",");
    }

    appendable
        .unindent()
        .appendLn("),")
        .append("BitPat(\"").append(toChiselPattern(0, signalsWidth)).appendLn("\"))")
        .unindent();

    appendable
        .appendLn("output := decoder(input, table)")
        .unindent().append("}");

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
   * Get the list of instructions in the decision tree.
   *
   * @param tree the decision tree
   * @return the list of instructions
   */
  private List<Instruction> getInstructions(Node tree) {
    if (tree instanceof LeafNode lf) {
      return List.of(lf.instruction());
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
    for (int i = 0; i < pattern.width(); i++) {
      sb.append(pattern.get(i).getValue() == PBit.Value.ONE ? '1' : (
          pattern.get(i).getValue() == PBit.Value.ZERO ? '0' : '?'));
    }
    return sb;
  }

  /**
   * Convert the given number to a Chisel bit pattern with zero-extension to the given width.
   *
   * @param num   The number
   * @param width The bit pattern width
   * @return The Chisel bit pattern
   */
  private CharSequence toChiselPattern(int num, int width) {
    var i = BigInteger.valueOf(num);
    var padding = StringUtils.repeat("0", width - i.bitLength());
    return "b" + padding + (num > 0 ? i.toString(2) : "");
  }
}
