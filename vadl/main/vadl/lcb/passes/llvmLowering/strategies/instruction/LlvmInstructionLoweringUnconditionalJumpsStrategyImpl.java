package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.isaMatching.MachineInstructionLabel.JAL;
import static vadl.viam.ViamError.ensurePresent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowering unconditional jump instructions into TableGen patterns.
 */
public class LlvmInstructionLoweringUnconditionalJumpsStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringUnconditionalJumpsStrategyImpl(ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(JAL);
  }

  @Override
  public Optional<LlvmLoweringRecord> lower(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      Graph uninlinedBehavior,
      @Nullable List<Graph> unmodifiedAdditionalBehaviors) {

    var visitor = replacementHooks();
    var copy = uninlinedBehavior.copy();

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    copy.deinitializeNodes();
    return Optional.of(
        createIntermediateResult(instruction, copy));
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithDefaultFieldAccessReplacement();
  }

  private LlvmLoweringRecord createIntermediateResult(
      Instruction instruction,
      Graph uninlinedGraph) {

    var outputOperands = getTableGenOutputOperands(uninlinedGraph);
    var inputOperands = getTableGenInputOperands(outputOperands, uninlinedGraph);
    var flags = getFlags(uninlinedGraph);

    var writes = uninlinedGraph.getNodes(WriteResourceNode.class).toList();
    var patterns = generatePatterns(instruction, uninlinedGraph, inputOperands, writes);

    var uses = getRegisterUses(uninlinedGraph, inputOperands, outputOperands);
    var defs = getRegisterDefs(uninlinedGraph, inputOperands, outputOperands);

    return new LlvmLoweringRecord(
        uninlinedGraph,
        inputOperands,
        outputOperands,
        flags,
        patterns,
        uses,
        defs
    );
  }


  @Override
  protected List<TableGenPattern> generatePatterns(Instruction instruction,
                                                   List<TableGenInstructionOperand> inputOperands,
                                                   List<WriteResourceNode> sideEffectNodes) {
    throw new RuntimeException("Must not be called. Use the other method");
  }

  protected List<TableGenPattern> generatePatterns(Instruction instruction,
                                                   Graph uninlinedGraph,
                                                   List<TableGenInstructionOperand> inputOperands,
                                                   List<WriteResourceNode> sideEffectNodes) {
    /*
    var selector = new Graph("selector", instruction);
    var machine = new Graph("machine", instruction);

    var imm = ensurePresent(uninlinedGraph.getNodes(FieldAccessRefNode.class).findFirst(),
        () -> Diagnostic.error("Instruction was expected to have an immediate.",
            instruction.sourceLocation()));
    var upcasted = ensurePresent(ValueType.from(imm.type()),
        () -> Diagnostic.error("Cannot convert immediate type to LLVM type.", imm.sourceLocation())
            .help("Check whether this type exists in LLVM"));

    selector.addWithInputs(
        new LlvmBrSD(new LlvmBasicBlockSD(imm.fieldAccess(), imm.type(), upcasted)));
    machine.addWithInputs(new MachineInstructionNode(
        new NodeList<>(
            new MachineInstructionParameterNode(new TableGenInstructionBareSymbolOperand(
                new LlvmBasicBlockSD(imm.fieldAccess(), imm.type(), upcasted), "type",
                imm.fieldAccess().simpleName()))

        ),
        instruction));

    return List.of(
        replaceBasicBlockByLabelImmediateInMachineInstruction(
            new TableGenSelectionWithOutputPattern(selector, machine)
        )
    );
     */
    return Collections.emptyList();
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
