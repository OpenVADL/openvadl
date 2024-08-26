package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.PredicateCodeGenerator;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
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

  record FrameIndexElimination(InstructionLabel instructionLabel,
                               Instruction instruction,
                               FieldAccessRefNode immediate,
      /*
       * Uses the predicate to check whether the offset
       * fits into register when eliminating frame index.
       */
                               String predicateMethodName) {

  }

  record Register(int index, String name) {

  }

  record RegisterClass(RegisterFile registerFile, List<Register> registers) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var instructionLabels =
        (HashMap<InstructionLabel, List<Instruction>>) passResults.getOfLastExecution(
            IsaMatchingPass.class).get();
    var uninlined = (IdentityHashMap<Instruction, UninlinedGraph>) passResults.getOfLastExecution(
        FunctionInlinerPass.class).get();
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "framePointer", abi.framePointer(),
        "stackPointer", abi.stackPointer(),
        "globalPointer", abi.globalPointer(),
        "frameIndexEliminations",
        getEliminateFrameIndexEntries(instructionLabels, uninlined).stream().sorted(
            Comparator.comparing(o -> o.instruction.identifier.name())).toList(),
        "registerClasses",
        specification.registerFiles().map(EmitRegisterInfoCppFilePass::getRegisterClass).toList());
  }

  @NotNull
  private static RegisterClass getRegisterClass(RegisterFile registerFile) {
    return new RegisterClass(registerFile,
        IntStream.range(0, (int) Math.pow(2, (long) registerFile.addressType().bitWidth()))
            .mapToObj(i -> new Register(i, registerFile.identifier.simpleName() + i))
            .toList()
    );
  }

  private List<FrameIndexElimination>
  getEliminateFrameIndexEntries(HashMap<InstructionLabel, List<Instruction>> instructionLabels,
                                IdentityHashMap<Instruction, UninlinedGraph> uninlined) {
    var entries = new ArrayList<FrameIndexElimination>();
    var affected =
        List.of(InstructionLabel.ADDI_32, InstructionLabel.STORE_MEM, InstructionLabel.LOAD_MEM);

    for (var label : affected) {
      for (var instruction : instructionLabels.getOrDefault(label, Collections.emptyList())) {
        var behavior = uninlined.get(instruction);
        ensureNonNull(behavior, "uninlined behavior is required");
        var immediate = behavior.getNodes(FieldAccessRefNode.class).findAny();
        ensure(immediate.isPresent(), "An immediate is required for frame index elimination");
        var entry = new FrameIndexElimination(label,
            instruction,
            immediate.get(),
            PredicateCodeGenerator.generateFunctionName(
                immediate.get().fieldAccess().predicate().name())
        );
        entries.add(entry);
      }
    }

    return entries;
  }
}
