package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.types.SIntType;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
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
  record FrameIndexElimination(MachineInstructionLabel machineInstructionLabel,
                               Instruction instruction,
                               FieldAccessRefNode immediate,
                               String predicateMethodName,
                               RegisterFile registerFile,
                               MachineInstructionIndices machineInstructionIndices,
                               long minValue,
                               long maxValue) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var instructionLabels =
        ((IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class)).labels();
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        ((FunctionInlinerPass.Output) passResults
            .lastResultOf(FunctionInlinerPass.class)).behaviors();
    var tableGenMachineInstructions = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var constraints = getConstraints(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
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
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .toList());
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
      @Nullable Map<MachineInstructionLabel, List<Instruction>> instructionLabels,
      @Nullable IdentityHashMap<Instruction, UninlinedGraph> uninlined,
      List<TableGenMachineInstruction> tableGenMachineInstructions) {
    ensureNonNull(instructionLabels, "labels must exist");
    ensureNonNull(uninlined, "uninlined must exist");

    var entries = new ArrayList<FrameIndexElimination>();
    var affected =
        List.of(MachineInstructionLabel.ADDI_32, MachineInstructionLabel.ADDI_64,
            MachineInstructionLabel.STORE_MEM,
            MachineInstructionLabel.LOAD_MEM);

    for (var label : affected) {
      for (var instruction : instructionLabels.getOrDefault(label, Collections.emptyList())) {
        var behavior = ensureNonNull(uninlined.get(instruction),
            () -> Diagnostic.error("No uninlined behavior was found.",
                instruction.sourceLocation()));
        var immediate = ensurePresent(behavior.getNodes(FieldAccessRefNode.class).findAny(), () ->
            Diagnostic.error("Cannot find an immediate for frame index elimination.",
                instruction.sourceLocation()));
        var indices =
            extractFrameIndexAndImmIndexFromMachineInstruction(tableGenMachineInstructions,
                instruction);
        var isSigned = immediate.fieldAccess().type() instanceof SIntType;
        var fieldBitWidth = immediate.fieldAccess().fieldRef().bitSlice().bitSize();
        long minValue = isSigned ? -1 * (long) Math.pow(2, fieldBitWidth - 1) : 0;
        long maxValue = isSigned ? (long) Math.pow(2, fieldBitWidth - 1) - 1 :
            (long) Math.pow(2, fieldBitWidth);
        var entry = new FrameIndexElimination(label, instruction, immediate,
            immediate.fieldAccess().predicate().identifier.lower(),
            instruction.behavior().getNodes(ReadRegFileNode.class).findFirst().get()
                .registerFile(), indices, minValue, maxValue);
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
            outputPattern.machine().getNodes(LcbMachineInstructionNode.class).findFirst().get();
        var nodeFI = rootNode.arguments().stream()
            .filter(x -> x instanceof LcbMachineInstructionParameterNode)
            .filter(
                x -> ((LcbMachineInstructionParameterNode) x).instructionOperand()
                    instanceof TableGenInstructionFrameRegisterOperand)
            .findFirst();
        var nodeImm = rootNode.arguments().stream()
            .filter(x -> x instanceof LcbMachineInstructionParameterNode)
            .filter(
                x -> ((LcbMachineInstructionParameterNode) x).instructionOperand()
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

    ensure(!machineInstructionIndices.isEmpty(), "Expected at least one FI pattern");
    return machineInstructionIndices.stream().findFirst().get();
  }
}
