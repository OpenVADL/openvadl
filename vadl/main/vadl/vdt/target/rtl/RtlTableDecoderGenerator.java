package vadl.vdt.target.rtl;

import static vadl.vdt.target.rtl.ChiselUtils.toChiselPattern;
import static vadl.vdt.utils.PatternUtils.toFixedBitPattern;

import java.nio.ByteOrder;
import java.util.Map;
import java.util.Set;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
import vadl.viam.Instruction;
import vadl.viam.Signal;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Generate decision logic for decoding fixed-length non-overlapping instructions to control
 * signals used within the RTL description.
 * <br> This uses Chisel's decoder library and built-in circuit optimization (qmc, espresso).
 */
public class RtlTableDecoderGenerator {

  private final CodeGeneratorAppendable appendable = new StringBuilderAppendable();

  /**
   * The name reference of the instruction word.
   */
  private final String input;

  /**
   * The decision to make if an instruction matches.
   */
  private final Map<Instruction, Map<Signal, ConstantNode>> decisionMap;

  /**
   * The available signals to assign.
   */
  private final Set<Signal> signals;

  /**
   * The signal (flag) representing the invalid instruction.
   */
  private final Signal invalidInsn;

  public RtlTableDecoderGenerator(
      String input,
      Map<Instruction, Map<Signal, ConstantNode>> decisionMap,
      Set<Signal> signals,
      Signal invalidInsn) {
    this.input = input;
    this.decisionMap = decisionMap;
    this.signals = signals;
    this.invalidInsn = invalidInsn;
  }

  /**
   * Generate the decision table for the RTL description.
   */
  public String generate() {

    var outputWidth =
        signals.stream().reduce(0, (a, b) -> a + b.type().asDataType().bitWidth(), Integer::sum);

    appendable.append("val dec_output = Wire(Bits(")
        .append(outputWidth)
        .appendLn(".W))")
        .newLine();

    // The template engine only indents the first line, so to format everything nicely, add an extra
    // indent level for everything else.
    appendable.indent();

    // Construct Chisel's decoder table
    appendable
        .appendLn("val table = TruthTable(")
        .indent()
        .appendLn("Map(")
        .indent();

    for (Map.Entry<Instruction, Map<Signal, ConstantNode>> decision : decisionMap.entrySet()) {

      var insn = decision.getKey();
      var pattern = toFixedBitPattern(insn, ByteOrder.LITTLE_ENDIAN);

      appendable
          .append("BitPat(\"").append(toChiselPattern(pattern, true)).append("\")")
          .append(" -> ")
          .append("BitPat(\"b");

      for (Signal signal : signals) {

        appendable.append(" ");

        var value = decision.getValue().get(signal);
        if (value != null) {
          appendable.append(value.constant().asVal().asString("", 2, true));
        } else {
          appendable.append(getDefaultValue(signal));
        }

      }

      appendable
          .append("\"), // ")
          .appendLn(insn.simpleName());
    }

    appendable
        .unindent()
        .append("), ")
        .append("BitPat(\"b");

    for (Signal signal : signals) {

      appendable.append(" ");

      if (isInvalid(signal)) {
        appendable.append("1");
      } else {
        appendable.append(getDefaultValue(signal));
      }

    }

    appendable
        .appendLn("\") // Invalid")
        .unindent()
        .appendLn(")")
        .newLine();

    appendable
        .appendLn("dec_output := decoder(" + input + ", table)")
        .newLine();

    int idx = outputWidth - 1;
    for (Signal signal : signals) {

      appendable
          .append(signal.simpleName())
          .append(" := dec_output(");

      appendable
          .append(idx)
          .append(", ")
          .append(idx - signal.type().asDataType().bitWidth() + 1);

      idx -= signal.type().asDataType().bitWidth();

      appendable.appendLn(")");

    }

    appendable.newLine();

    return appendable.toString();
  }

  private boolean isInvalid(Signal signal) {
    return signal.identifier.equals(invalidInsn.identifier);
  }

  public static CharSequence getDefaultValue(Signal signal) {

    if (!signal.type().isDataType()) {
      throw new ViamError("Signal type not supported: %s", signal.type());
    }

    return "0".repeat(signal.type().asDataType().bitWidth());
  }
}
