package vadl.lcb.passes.llvmLowering.domain;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

/**
 * Expands {@link LlvmLoweringRecord} by the updated behaviors of the target instructions.
 */
public class LlvmLoweringPseudoRecord extends LlvmLoweringRecord {

  private final IdentityHashMap<Instruction, Graph> appliedBehaviors;

  public LlvmLoweringPseudoRecord(Graph behavior,
                                  List<TableGenInstructionOperand> inputs,
                                  List<TableGenInstructionOperand> outputs,
                                  LlvmLoweringPass.Flags flags,
                                  List<TableGenPattern> patterns,
                                  List<RegisterRef> uses,
                                  List<RegisterRef> def,
                                  IdentityHashMap<Instruction, Graph> appliedBehaviors) {
    super(behavior, inputs, outputs, flags, patterns, uses, def);
    this.appliedBehaviors = appliedBehaviors;
  }


  public LlvmLoweringPseudoRecord(LlvmLoweringPseudoRecord base,
                                  List<TableGenInstructionOperand> inputs) {
    super(base.behavior(), inputs, base.outputs(), base.flags(), base.patterns(), base.uses(),
        base.defs());
    this.appliedBehaviors = base.appliedBehaviors;
  }

  public Map<Instruction, Graph> appliedBehaviors() {
    return appliedBehaviors;
  }
}
