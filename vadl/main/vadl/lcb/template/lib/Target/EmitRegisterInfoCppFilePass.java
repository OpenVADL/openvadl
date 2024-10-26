package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.passes.dummyAbi.DummyAbi;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * This file contains the register definitions for compiler backend.
 */
public class EmitRegisterInfoCppFilePass extends LcbTemplateRenderingPass {

  public EmitRegisterInfoCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/RegisterInfo.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName + "RegisterInfo.cpp";
  }

  /**
   * The ADDI and memory manipulation instructions will handle the frame index.
   * Therefore, LLVM requires methods to eliminate the index. An object of this
   * record represents one method for each {@link Instruction} (ADDI, MEM_STORE, MEM_LOAD).
   */
  record FrameIndexElimination(InstructionLabel instructionLabel,
                               Instruction instruction,
                               FieldAccessRefNode immediate,
                               String predicateMethodName,
                               RegisterFile registerFile,
                               MachineInstructionIndices machineInstructionIndices) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var instructionLabels = (HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMatchingPass.class);
    var uninlined = (IdentityHashMap<Instruction, UninlinedGraph>) passResults.lastResultOf(
        FunctionInlinerPass.class);
    var tableGenMachineInstructions = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var constraints = getConstraints(specification);
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "constraints", constraints,
        "framePointer", abi.framePointer(),
        "returnAddress", abi.returnAddress(),
        "stackPointer", abi.stackPointer(),
        "globalPointer",
        abi.globalPointer(), "frameIndexEliminations",
        getEliminateFrameIndexEntries(instructionLabels, uninlined,
            tableGenMachineInstructions).stream()
            .sorted(Comparator.comparing(o -> o.instruction.identifier.name())).toList(),
        "registerClasses",
        specification.registerFiles().map(RegisterUtils::getRegisterClass).toList());
  }

  record ReservedRegister(String registerFile, int index) {

  }

  private List<ReservedRegister> getConstraints(Specification specification) {
    var reserved = new ArrayList<ReservedRegister>();
    var registerFiles = specification.registerFiles().toList();

    for (var registerFile : registerFiles) {
      for (var constraint : registerFile.constraints()) {
        reserved.add(
            new ReservedRegister(registerFile.identifier.simpleName(),
                constraint.address().intValue()));
      }
    }

    return reserved;
  }

  /**
   * Stores indices in the machine instruction.
   *
   * @param indexFI          is the frame index in the instruction.
   * @param indexImm         is the index of the immediate in the instruction.
   * @param relativeDistance is the distance between immediate operand and frame index. It is
   *                         easier to store the relative distance because LLVM has utility
   *                         functions to extract the frame index. Otherwise, we would need
   *                         to offset the indexImm by the number of output operands in the
   *                         machine instructions to eliminate the frame index.
   */
  record MachineInstructionIndices(int indexFI, int indexImm, int relativeDistance) {

  }

  private List<FrameIndexElimination> getEliminateFrameIndexEntries(
      @Nullable Map<InstructionLabel, List<Instruction>> instructionLabels,
      @Nullable IdentityHashMap<Instruction, UninlinedGraph> uninlined,
      List<TableGenMachineInstruction> tableGenMachineInstructions) {
    ensureNonNull(instructionLabels, "labels must exist");
    ensureNonNull(uninlined, "uninlined must exist");

    var entries = new ArrayList<FrameIndexElimination>();
    var affected =
        List.of(InstructionLabel.ADDI_32, InstructionLabel.ADDI_64, InstructionLabel.STORE_MEM,
            InstructionLabel.LOAD_MEM);

    for (var label : affected) {
      for (var instruction : instructionLabels.getOrDefault(label, Collections.emptyList())) {
        var behavior = uninlined.get(instruction);
        ensureNonNull(behavior, "uninlined behavior is required");
        var immediate = behavior.getNodes(FieldAccessRefNode.class).findAny();
        ensure(immediate.isPresent(), "An immediate is required for frame index elimination");
        var indices =
            extractFrameIndexAndImmIndexFromMachineInstruction(tableGenMachineInstructions,
                instruction);
        var entry = new FrameIndexElimination(label, instruction, immediate.get(),
            immediate.get().fieldAccess().predicate().identifier.lower(),
            instruction.behavior().getNodes(ReadRegFileNode.class).findFirst().get()
                .registerFile(), indices);
        entries.add(entry);
      }
    }

    return entries;
  }

  private MachineInstructionIndices extractFrameIndexAndImmIndexFromMachineInstruction(
      List<TableGenMachineInstruction> tableGenMachineInstructions, Instruction instruction) {
    var machineInstructionIndices = new ArrayList<MachineInstructionIndices>();

    var record = ensurePresent(
        tableGenMachineInstructions.stream().filter(x -> x.instruction() == instruction)
            .findFirst(),
        "Cannot find a tablegen record for this machine instruction");

    for (var pattern : record.getAnonymousPatterns()) {
      if (pattern instanceof TableGenSelectionWithOutputPattern outputPattern) {
        var rootNode =
            outputPattern.machine().getNodes(MachineInstructionNode.class).findFirst().get();
        var nodeFI = rootNode.arguments().stream()
            .filter(x -> x instanceof MachineInstructionParameterNode)
            .filter(
                x -> ((MachineInstructionParameterNode) x).instructionOperand()
                    instanceof TableGenInstructionFrameRegisterOperand)
            .findFirst();
        var nodeImm = rootNode.arguments().stream()
            .filter(x -> x instanceof MachineInstructionParameterNode)
            .filter(
                x -> ((MachineInstructionParameterNode) x).instructionOperand()
                    instanceof TableGenInstructionImmediateOperand)
            .findFirst();

        if (nodeFI.isPresent() && nodeImm.isPresent()) {
          int indexFI = rootNode.arguments().indexOf(nodeFI.get());
          int indexImm = rootNode.arguments().indexOf(nodeImm.get());

          machineInstructionIndices.add(
              new MachineInstructionIndices(indexFI, indexImm, indexImm - indexFI));
        }
      }
    }

    ensure(machineInstructionIndices.size() > 0, "Expected at least one FI pattern");
    return machineInstructionIndices.stream().findFirst().get();
  }
}
