package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file contains the legalization, promotions and legalization of nodes.
 */
public class EmitISelLoweringCppFilePass extends LcbTemplateRenderingPass {

  public EmitISelLoweringCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/ISelLowering.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "ISelLowering.cpp";
  }

  static class LlvmRegisterFile extends RegisterFile {

    /**
     * Constructs a new RegisterFile object.
     **/
    public LlvmRegisterFile(RegisterFile registerFile) {
      super(registerFile.identifier, registerFile.addressType(), registerFile.resultType(),
          registerFile.constraints());
    }

    public String llvmResultType() {
      return ValueType.from(type()).get().getLlvmType();
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var registerFiles = ((GenerateRegisterClassesPass.Output) passResults.lastResultOf(
        GenerateRegisterClassesPass.class)).registerClasses();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var addressSequence = abi.addressSequence();
    var labelledMachineInstructions = ensureNonNull(
        (HashMap<MachineInstructionLabel, List<Instruction>>) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions",
            specification.sourceLocation()));
    var hasCMove32 = labelledMachineInstructions.containsKey(MachineInstructionLabel.CMOVE_32);
    var hasCMove64 = labelledMachineInstructions.containsKey(MachineInstructionLabel.CMOVE_64);
    var conditionalMove = getConditionalMove(hasCMove32, hasCMove64, labelledMachineInstructions);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, specification.simpleName());
    map.put("registerFiles", registerFiles);
    map.put("framePointer", framePointer);
    map.put("stackPointer", stackPointer);
    map.put("stackPointerByteSize", abi.stackPointer().registerFile().resultType().bitWidth() / 8);
    map.put("argumentRegisterClasses", abi.argumentRegisters().stream().map(
            DummyAbi.RegisterRef::registerFile)
        .distinct()
        .map(LlvmRegisterFile::new)
        .toList());
    map.put("argumentRegisters", abi.argumentRegisters());
    map.put("stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType());
    map.put("addressSequence", addressSequence);
    map.put("hasCMove32", hasCMove32);
    map.put("hasCMove64", hasCMove64);
    map.put("conditionalMove", conditionalMove);
    map.put("branchInstructions", getBranchInstructions(labelledMachineInstructions));
    return map;
  }

  record BranchInstruction(String instructionName, String isdName) {

  }

  private List<BranchInstruction> getBranchInstructions(
      HashMap<MachineInstructionLabel, List<Instruction>>
          labelledMachineInstructions) {
    var result = new ArrayList<BranchInstruction>();
    var branchInstructions = Set.of(
        MachineInstructionLabel.BEQ,
        MachineInstructionLabel.BSGEQ,
        MachineInstructionLabel.BSGTH,
        MachineInstructionLabel.BSLEQ,
        MachineInstructionLabel.BSLTH,
        MachineInstructionLabel.BUGEQ,
        MachineInstructionLabel.BUGTH,
        MachineInstructionLabel.BULEQ,
        MachineInstructionLabel.BULTH,
        MachineInstructionLabel.BNEQ
    );
    var translation = new HashMap<MachineInstructionLabel, String>();
    translation.put(MachineInstructionLabel.BEQ, "SETEQ");
    translation.put(MachineInstructionLabel.BSGEQ, "SETGE");
    translation.put(MachineInstructionLabel.BSGTH, "SETGT");
    translation.put(MachineInstructionLabel.BSLEQ, "SETLE");
    translation.put(MachineInstructionLabel.BSLTH, "SETLT");
    translation.put(MachineInstructionLabel.BUGEQ, "SETUGE");
    translation.put(MachineInstructionLabel.BUGTH, "SETUGT");
    translation.put(MachineInstructionLabel.BULEQ, "SETULE");
    translation.put(MachineInstructionLabel.BULTH, "SETULT");
    translation.put(MachineInstructionLabel.BNEQ, "SETNE");

    branchInstructions.forEach(bi -> labelledMachineInstructions.compute(bi,
        (key, value) -> {
          if (value != null) {
            var instruction = getFirstInstruction(value);
            result.add(new BranchInstruction(instruction.simpleName(),
                Objects.requireNonNull(translation.get(key))));
          }
          return null;
        }));

    return result;
  }

  private static @NotNull Instruction getFirstInstruction(List<Instruction> value) {
    ensureNonNull(value, "Must not be null");
    var instruction =
        ensurePresent(value.stream().findFirst(), "At least one item must exist");
    return instruction;
  }

  @Nullable
  private Instruction getConditionalMove(boolean hasCMove32, boolean hasCMove64,
                                         HashMap<MachineInstructionLabel, List<Instruction>>
                                             labelledMachineInstructions) {
    if (hasCMove64) {
      var cmove = labelledMachineInstructions.get(MachineInstructionLabel.CMOVE_32);
      ensureNonNull(cmove, "must not be null");
      return ensurePresent(
          cmove.stream().findFirst(),
          "At least one element should be present");
    } else if (hasCMove32) {
      var cmove = labelledMachineInstructions.get(MachineInstructionLabel.CMOVE_64);
      ensureNonNull(cmove, "must not be null");
      return ensurePresent(
          cmove.stream().findFirst(),
          "At least one element should be present");
    }

    return null;
  }
}
