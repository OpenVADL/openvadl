package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

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
                               RegisterFile registerFile) {

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
    var constraints = getConstraints(specification);
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "constraints", constraints,
        "framePointer", abi.framePointer(),
        "returnAddress", abi.returnAddress(),
        "stackPointer", abi.stackPointer(),
        "globalPointer",
        abi.globalPointer(), "frameIndexEliminations",
        getEliminateFrameIndexEntries(instructionLabels, uninlined).stream()
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

  private List<FrameIndexElimination> getEliminateFrameIndexEntries(
      @Nullable HashMap<InstructionLabel, List<Instruction>> instructionLabels,
      @Nullable IdentityHashMap<Instruction, UninlinedGraph> uninlined) {
    ensureNonNull(instructionLabels, "labels must exist");
    ensureNonNull(uninlined, "uninlined must exist");

    var entries = new ArrayList<FrameIndexElimination>();
    var affected =
        List.of(InstructionLabel.ADDI_32, InstructionLabel.STORE_MEM, InstructionLabel.LOAD_MEM);

    for (var label : affected) {
      for (var instruction : instructionLabels.getOrDefault(label, Collections.emptyList())) {
        var behavior = uninlined.get(instruction);
        ensureNonNull(behavior, "uninlined behavior is required");
        var immediate = behavior.getNodes(FieldAccessRefNode.class).findAny();
        ensure(immediate.isPresent(), "An immediate is required for frame index elimination");
        var entry = new FrameIndexElimination(label, instruction, immediate.get(),
            immediate.get().fieldAccess().predicate().identifier.lower(),
            instruction.behavior().getNodes(ReadRegFileNode.class).findFirst().get()
                .registerFile());
        entries.add(entry);
      }
    }

    return entries;
  }
}
