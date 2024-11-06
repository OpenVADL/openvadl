package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.llvmLowering.strategies.LoweringStrategyUtils.replaceBasicBlockByLabelImmediateInMachineInstruction;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringPseudoRecord;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.PseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionBareSymbolOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;

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
  public Optional<LlvmLoweringPseudoRecord> lower(
      PseudoInstruction pseudo,
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions) {
    var tableGenRecord = super.lower(pseudo, labelledMachineInstructions);

    if (tableGenRecord.isPresent()) {
      /* The unconditional jump requires an immediate which is the basic block which
       we should jump to. However, the pseudo instruction's machine instruction has no immediate
       because it is replaced by a `PseudoFuncParam`.

       pseudo instruction J( offset : Bits<20> ) =
       {
          JAL{ rd = 0 as Bits5, imm = offset }
       }

       Here, for example, you have a pseudo instruction where the `imm` is set with `offset` which
       is not an immediate.
       We solve this problem by looking at `JAL`'s format and seeing that there is only one field
       access function. This approach will only work when there is *only* one field access function.
       Furthermore, the solution has two parts: generating patterns and generating the input
       operands. The pattern part is covered in `generatePatternVariations`. The input operand
       is overwritten here.
       */

      var instrCallNode = getInstrCallNodeOrThrowError(pseudo);
      var fieldAccess = getFieldAccessFunctionFromFormatOrThrowError(instrCallNode);
      var fieldAccessNode = new LlvmFieldAccessRefNode(fieldAccess,
          fieldAccess.type(),
          upcastFieldAccess(fieldAccess),
          LlvmFieldAccessRefNode.Usage.BasicBlock);
      var inputOperand =
          LlvmInstructionLoweringStrategy.generateTableGenInputOutput(fieldAccessNode);
      return Optional.of(new LlvmLoweringPseudoRecord(tableGenRecord.get(), List.of(inputOperand)));
    } else {
      return Optional.empty();
    }
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

    var instrCallNode = getInstrCallNodeOrThrowError(pseudo);
    var appliedGraph =
        Objects.requireNonNull(appliedInstructionBehavior.get(instrCallNode.target()));
    ensureNonNull(appliedGraph,
        () -> Diagnostic.error("Machine instruction is not part of the pseudo instruction",
            instrCallNode.sourceLocation()));
    var fieldAccess = getFieldAccessFunctionFromFormatOrThrowError(instrCallNode);
    var upcasted = upcastFieldAccess(fieldAccess);

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

  private static @NotNull ValueType upcastFieldAccess(Format.FieldAccess fieldAccess) {
    return ensurePresent(ValueType.from(fieldAccess.type()),
        () -> Diagnostic.error("Cannot convert immediate type to LLVM type.",
                fieldAccess.sourceLocation())
            .help("Check whether this type exists in LLVM"));
  }

  private static @NotNull InstrCallNode getInstrCallNodeOrThrowError(PseudoInstruction pseudo) {
    ensure(pseudo.behavior().getNodes(InstrCallNode.class).count() == 1,
        () -> Diagnostic.error("Expected only one machine instruction",
            pseudo.sourceLocation()));
    return
        ensurePresent(pseudo.behavior().getNodes(InstrCallNode.class).findFirst(),
            () -> Diagnostic.error("Expected only one machine instruction",
                pseudo.sourceLocation()));
  }

  private static Format.@NotNull FieldAccess getFieldAccessFunctionFromFormatOrThrowError(
      InstrCallNode machineInstruction) {
    ensure(machineInstruction.target().format().fieldAccesses().size() == 1, () ->
        Diagnostic.error(
            "Machine instruction must only have one field access function to be able to "
                + "deduce the immediate layout for the machine instruction.",
            machineInstruction.sourceLocation()));
    return
        ensurePresent(machineInstruction.target().format().fieldAccesses().stream().findFirst(),
            () -> Diagnostic.error("Cannot find a field access function",
                machineInstruction.sourceLocation()));
  }
}
