package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BGEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BGTH;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BLEQ;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BLTH;
import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.BNEQ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LoweringStrategyUtils;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowering conditional branch instructions into TableGen patterns.
 */
public class LlvmInstructionLoweringConditionalBranchesStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringConditionalBranchesStrategyImpl(ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(BEQ, BGEQ, BNEQ, BLEQ, BLTH, BGTH);
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithFieldAccessWithBasicBlockReplacement();
  }

  @Override
  public Optional<LlvmLoweringRecord> lower(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      UninlinedGraph uninlinedBehavior) {

    var visitor = replacementHooks();
    var copy = (UninlinedGraph) uninlinedBehavior.copy();

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    copy.deinitializeNodes();
    return Optional.of(
        createIntermediateResult(labelledMachineInstructions, instruction, copy));
  }

  private LlvmLoweringRecord createIntermediateResult(
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      UninlinedGraph visitedGraph) {

    var outputOperands = getTableGenOutputOperands(visitedGraph);
    var inputOperands = getTableGenInputOperands(outputOperands, visitedGraph);
    var flags = getFlags(visitedGraph);

    var writes = visitedGraph.getNodes(WriteResourceNode.class).toList();
    var patterns = generatePatterns(instruction, inputOperands, writes);
    var alternatives =
        generatePatternVariations(instruction,
            supportedInstructions,
            visitedGraph,
            inputOperands, outputOperands, patterns);

    var allPatterns = Stream.concat(patterns.stream(), alternatives.stream())
        .map(LoweringStrategyUtils::replaceBasicBlockByLabelImmediateInMachineInstruction)
        .toList();

    var uses = getRegisterUses(visitedGraph, inputOperands, outputOperands);
    var defs = getRegisterDefs(visitedGraph, inputOperands, outputOperands);

    return new LlvmLoweringRecord(
        visitedGraph,
        inputOperands,
        outputOperands,
        flags,
        allPatterns,
        uses,
        defs
    );
  }


  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
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

    for (var pattern : patterns.stream()
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x).toList()) {
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

        // We also extend the result of the condition to i32 or i64.
        var typeCast = new LlvmTypeCastSD(builtinCall, node.immOffset().type());
        var brCond = new LlvmBrCondSD(typeCast, node.immOffset());
        node.replaceAndDelete(brCond);
        hasChanged = true;
      }

      // If nothing had changed, then it makes no sense to add it.
      if (hasChanged) {
        alternatives.add(new TableGenSelectionWithOutputPattern(selector, machine));
      }
    }

    return alternatives;
  }


}
