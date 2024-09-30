package vadl.lcb.passes.llvmLowering.strategies;

import static vadl.viam.ViamError.ensure;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.PseudoFuncParamNode;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link PseudoInstruction}.
 */
public class LlvmPseudoLoweringImpl {

  public Optional<LlvmLoweringRecord> lower(PseudoInstruction pseudo) {
    ensure(!pseudo.identifier.simpleName().equals("RESERVERD_PSEUDO_RET"),
        () -> Diagnostic.error("The name of the pseudo instruction is reserved.",
            pseudo.identifier.sourceLocation()).build());

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
            var argument = indexArgument(callNode.arguments(), app.right());

            /*
              pseudo instruction MOV( rd : Index, rs1 : Index ) =
              {
                  ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits12 }
              }
             */
            instructionBehavior.getNodes(FieldRefNode.class)
                .filter(x -> x.formatField() == formatField)
                .forEach(occurrence -> {
                  occurrence.replaceAndDelete(argument.copy());
                });
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

  /**
   * There are two relevant cases.
   * The first is that the {@code argument} is a constant. Then, we do not have to do anything.
   * The second case is when {@link PseudoInstruction} uses an {@code index}. Then, the argument
   * is replaced by a {@link FuncParamNode}. However, we still require to know the index for
   * the pseudo instance expansion. That's why we extend {@link FuncParamNode} with
   * {@link PseudoFuncParamNode} which has an {@code index} property.
   * Here is an example of the index. Note that {@code rs} will be transformed into
   * a {@link PseudoFuncParamNode} when it is replaced.
   * <code>
   * pseudo instruction BGEZ( rs : Index, offset : Bits<12> ) =
   * {
   * BGE{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
   * }
   * </code>
   */
  private ExpressionNode indexArgument(List<ExpressionNode> arguments, ExpressionNode argument) {
    if (argument instanceof FuncParamNode funcParamNode) {
      int index = arguments.indexOf(argument);
      return new PseudoFuncParamNode(funcParamNode.parameter(), index);
    }
    return argument;
  }
}
