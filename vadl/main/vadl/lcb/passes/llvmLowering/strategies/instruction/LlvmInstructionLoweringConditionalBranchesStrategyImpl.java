package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.isaMatching.InstructionLabel.BEQ;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BGEQ;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BGTH;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BLEQ;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BLTH;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BNEQ;
import static vadl.viam.ViamError.ensure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.model.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.model.LlvmSetCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.visitors.impl.ReplaceWithLlvmSDNodesWithControlFlowVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.ParameterIdentity;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.visitors.LcbGraphNodeVisitor;
import vadl.types.Type;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowering conditional branch instructions into TableGen patterns.
 */
public class LlvmInstructionLoweringConditionalBranchesStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(BEQ, BGEQ, BNEQ, BLEQ, BLTH, BGTH);
  }

  @Override
  protected LcbGraphNodeVisitor getVisitorForPatternSelectorLowering() {
    // Branch instructions contain if conditionals.
    // The normal visitor denies those. But "xxxWithControlFlowVisitor" we are allowing
    // these instructions for conditional branches.
    return new ReplaceWithLlvmSDNodesWithControlFlowVisitor();
  }

  @Override
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      InstructionLabel instructionLabel,
      UninlinedGraph uninlinedBehavior) {

    var visitor = getVisitorForPatternSelectorLowering();
    var copy = (UninlinedGraph) uninlinedBehavior.copy();

    for (var node : copy.getNodes().toList()) {
      visitor.visit(node);
    }

    copy.deinitializeNodes();
    return Optional.of(
        createIntermediateResult(supportedInstructions, instruction, instructionLabel, copy));
  }

  private LlvmLoweringPass.LlvmLoweringIntermediateResult createIntermediateResult(
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      InstructionLabel instructionLabel,
      UninlinedGraph visitedGraph) {

    var inputOperands = getTableGenInputOperands(visitedGraph);
    var outputOperands = getTableGenOutputOperands(visitedGraph);
    var flags = getFlags(visitedGraph);

    var writes = visitedGraph.getNodes(WriteResourceNode.class).toList();
    var patterns = generatePatterns(instruction, inputOperands, writes);
    var alternatives =
        generatePatternVariations(instruction, supportedInstructions, instructionLabel,
            visitedGraph,
            inputOperands, outputOperands, patterns);

    var allPatterns = Stream.concat(patterns.stream(), alternatives.stream())
        .map(this::replaceBasicBlockByLabelImmediateInMachineInstruction)
        .toList();

    return new LlvmLoweringPass.LlvmLoweringIntermediateResult(
        visitedGraph,
        inputOperands,
        outputOperands,
        flags,
        allPatterns,
        getRegisterUses(visitedGraph),
        getRegisterDefs(visitedGraph)
    );
  }

  /**
   * Conditional branch patterns reference the {@code bb} selection dag node. However,
   * the machine instruction should use the label immediate to properly encode the instruction.
   */
  private TableGenPattern replaceBasicBlockByLabelImmediateInMachineInstruction(
      TableGenPattern pattern) {
    // We know that the `selector` already has LlvmBasicBlock nodes.
    var candidates = pattern.machine().getNodes(MachineInstructionParameterNode.class).toList();
    for (var candidate : candidates) {
      if (candidate.instructionOperand().origin() instanceof LlvmBasicBlockSD basicBlockSD) {
        candidate.setInstructionOperand(new TableGenInstructionImmediateLabelOperand(
            ParameterIdentity.fromBasicBlockToImmediateLabel(basicBlockSD), basicBlockSD));
      }
    }

    return pattern;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      UninlinedGraph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    ArrayList<TableGenPattern> alternatives = new ArrayList<>();

    // Generate brcond patterns from brcc
    /*
    def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
          (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

    to

    def : Pat<(brcond (i32 (seteq X:$rs1, X:$rs2)), bb:$imm12),
        (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;
     */

    for (var pattern : patterns) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      // Replace BrCc with BrCond
      var hasChanged = false;
      for (var node : selector.getNodes(LlvmBrCcSD.class).toList()) {
        // For `brcc` we have Setcc code, so need to see if we have a suitable
        // instruction for that.
        var builtin = LlvmCondCode.from(node.condition());
        var builtinCall =
            new LlvmSetCondSD(builtin, new NodeList<>(node.first(), node.second()),
                node.first().type());

        // We also extend the result of the condition to i32.
        var typeCast = new LlvmTypeCastSD(builtinCall, Type.signedInt(32));
        var brCond = new LlvmBrCondSD(typeCast, node.immOffset());
        node.replaceAndDelete(brCond);
        hasChanged = true;
      }

      // If nothing had changed, then it makes no sense to add it.
      if (hasChanged) {
        alternatives.add(new TableGenPattern(selector, machine));
      }
    }

    return alternatives;
  }


}
