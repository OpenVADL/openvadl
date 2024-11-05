package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.llvmLowering.strategies.LoweringStrategyUtils.replaceBasicBlockByLabelImmediateInMachineInstruction;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jdk.jshell.Diag;
import vadl.error.Diagnostic;
import vadl.gcb.passes.pseudo.PseudoFuncParamNode;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.PseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionBareSymbolOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * Lowers unconditional jumps into TableGen.
 */
public class LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl extends
    LlvmPseudoInstructionLowerStrategy {
  /**
   * Constructor.
   */
  public LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }

  @Override
  protected Set<PseudoInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(PseudoInstructionLabel.J);
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      PseudoInstruction pseudo,
      LlvmLoweringRecord record,
      IdentityHashMap<Instruction, Graph> appliedInstructionBehavior) {
    /*
    def : Pat<(br bb:$offset),
          (J RV32I_Jtype_ImmediateJ_immediateAsLabel:$offset)>;
     */
    var selector = new Graph("selector");
    var machine = new Graph("selector");

    ensure(pseudo.behavior().getNodes(InstrCallNode.class).count() == 1,
        () -> Diagnostic.error("Expected only one machine instruction",
            pseudo.sourceLocation()));
    var machineInstruction =
        ensurePresent(pseudo.behavior().getNodes(InstrCallNode.class).findFirst(),
            () -> Diagnostic.error("Expected only one machine instruction",
                pseudo.sourceLocation()));
    var appliedGraph =
        Objects.requireNonNull(appliedInstructionBehavior.get(machineInstruction.target()));
    ensureNonNull(appliedGraph,
        () -> Diagnostic.error("Machine instruction is not part of the pseudo instruction",
            machineInstruction.sourceLocation()));
    ensure(machineInstruction.target().format().fieldAccesses().size() == 1, () ->
        Diagnostic.error(
            "Machine instruction must only have one field access function to be able to "
                + "deduce the immediate layout for the machine instruction.",
            machineInstruction.sourceLocation()));

    var fieldAccess =
        ensurePresent(machineInstruction.target().format().fieldAccesses().stream().findFirst(),
            () -> Diagnostic.error("Cannot find a field access function",
                machineInstruction.sourceLocation()));

    var upcasted = ensurePresent(ValueType.from(fieldAccess.type()),
        () -> Diagnostic.error("Cannot convert immediate type to LLVM type.",
                fieldAccess.sourceLocation())
            .help("Check whether this type exists in LLVM"));

    selector.addWithInputs(
        new LlvmBrSD(new LlvmBasicBlockSD(fieldAccess, fieldAccess.type(), upcasted)));
    machine.addWithInputs(new PseudoInstructionNode(
        new NodeList<>(
            new MachineInstructionParameterNode(new TableGenInstructionBareSymbolOperand(
                new LlvmBasicBlockSD(fieldAccess, fieldAccess.type(), upcasted), "type",
                fieldAccess.simpleName()))

        ), pseudo));

    return List.of(
        replaceBasicBlockByLabelImmediateInMachineInstruction(
            new TableGenSelectionWithOutputPattern(selector, machine)
        )
    );
  }
}
