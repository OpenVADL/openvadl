package vadl.lcb.passes.llvmLowering.strategies;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Register;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link PseudoInstruction}.
 */
public class LlvmPseudoLoweringImpl {

  public Optional<LlvmLoweringRecord> lower(PseudoInstruction pseudo) {
    var uses = new ArrayList<RegisterRef>();
    var defs = new ArrayList<RegisterRef>();
    var inputOperands = new ArrayList<TableGenInstructionOperand>();
    var outputOperands = new ArrayList<TableGenInstructionOperand>();

    var isTerminator = false;
    var isReturn = false;
    var mayLoad = false;
    var mayStore = false;
    var isBranch = false;

    for (var callNode : pseudo.behavior().getNodes(InstrCallNode.class).toList()) {
      var instructionBehavior = callNode.target().behavior().copy();

      /*
      Example:
      pseudo instruction RET =
      {
          JALR{ rs1 = 1 as Bits5, rd = 0 as Bits5, imm = 0 as Bits12 }
      }
      */

      // Apply the argument from pseudo instruction.
      Streams.zip(callNode.getParamFields().stream(), callNode.arguments().stream(),
              Pair::new)
          .forEach(app -> {
            var formatField = app.left();
            var argument = app.right();

            /*
              We are only inlining constants.
              Here we would ignore `rd` and `rs1` so they remain fields in the graph.

              pseudo instruction MOV( rd : Index, rs1 : Index ) =
              {
                  ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits12 }
              }
             */
            if (argument instanceof ConstantNode) {
              // TODO(kper): emit a warning when not replaced.
              instructionBehavior.getNodes(FieldRefNode.class)
                  .filter(x -> x.formatField() == formatField)
                  .forEach(occurrence -> occurrence.replaceAndDelete(argument.copy()));
            }
          });

      uses.addAll(LlvmInstructionLoweringStrategy.getRegisterUses(instructionBehavior));
      defs.addAll(LlvmInstructionLoweringStrategy.getRegisterDefs(instructionBehavior));
      inputOperands.addAll(
          LlvmInstructionLoweringStrategy.getTableGenInputOperands(instructionBehavior));
      outputOperands.addAll(
          LlvmInstructionLoweringStrategy.getTableGenOutputOperands(instructionBehavior));

      var flags = LlvmInstructionLoweringStrategy.getFlags(instructionBehavior);
      isTerminator |= flags.isTerminator();
      isReturn |= flags.isReturn();
      mayLoad |= flags.mayLoad();
      mayStore |= flags.mayStore();
      isBranch |= flags.isBranch();
    }

    var flags = new LlvmLoweringPass.Flags(isTerminator,
        isBranch,
        false,
        isReturn,
        true,
        false,
        mayLoad,
        mayStore);

    return Optional.of(new LlvmLoweringRecord(pseudo.behavior(),
        inputOperands,
        outputOperands,
        flags,
        Collections.emptyList(),
        uses,
        defs
    ));
  }
}
