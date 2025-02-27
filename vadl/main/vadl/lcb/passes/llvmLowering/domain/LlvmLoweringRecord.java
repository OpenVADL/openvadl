package vadl.lcb.passes.llvmLowering.domain;


import java.util.List;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Format;
import vadl.viam.graph.Graph;

/**
 * Contains information for the lowering of instructions.
 */
public class LlvmLoweringRecord {
  private final Graph behavior;
  private final List<TableGenInstructionOperand> inputs;
  private final List<TableGenInstructionOperand> outputs;
  private final LlvmLoweringPass.Flags flags;
  private final List<TableGenPattern> patterns;
  private final List<RegisterRef> uses;
  private final List<RegisterRef> def;

  /**
   * Constructor.
   */
  public LlvmLoweringRecord(Graph behavior, List<TableGenInstructionOperand> inputs,
                            List<TableGenInstructionOperand> outputs, LlvmLoweringPass.Flags flags,
                            List<TableGenPattern> patterns, List<RegisterRef> uses,
                            List<RegisterRef> def) {
    this.behavior = behavior;
    this.inputs = inputs;
    this.outputs = outputs;
    this.flags = flags;
    this.patterns = patterns;
    this.uses = uses;
    this.def = def;
  }

  public Graph behavior() {
    return behavior;
  }

  public List<TableGenInstructionOperand> inputs() {
    return inputs;
  }

  public List<TableGenInstructionOperand> outputs() {
    return outputs;
  }

  public LlvmLoweringPass.Flags flags() {
    return flags;
  }

  public List<TableGenPattern> patterns() {
    return patterns;
  }

  public List<RegisterRef> uses() {
    return uses;
  }

  public List<RegisterRef> defs() {
    return def;
  }

  /**
   * Find the index in the {@link #inputs} by the given field.
   */
  public int findInputIndex(Format.Field field) {
    for (int i = 0; i < inputs.size(); i++) {
      if (inputs.get(i) instanceof ReferencesFormatField x && x.formatField() == field) {
        return i;
      }
    }

    throw Diagnostic.error("Cannot find field in inputs.", field.sourceLocation()).build();
  }
}
