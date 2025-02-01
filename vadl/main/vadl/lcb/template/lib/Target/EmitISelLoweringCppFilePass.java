package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.MachineInstructionLabelGroup;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * This file contains the legalization, promotions and legalization of nodes.
 */
public class EmitISelLoweringCppFilePass extends LcbTemplateRenderingPass {

  public EmitISelLoweringCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/ISelLowering.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName + "ISelLowering.cpp";
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
    var abi = (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var registerFiles = ((GenerateRegisterClassesPass.Output) passResults.lastResultOf(
        GenerateRegisterClassesPass.class)).registerClasses();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var addressSequence = abi.addressSequence();
    var labelledMachineInstructions = ensureNonNull(
        (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions",
            specification.sourceLocation()))
        .labels();
    var hasCMove32 = labelledMachineInstructions.containsKey(MachineInstructionLabel.CMOVE_32);
    var hasCMove64 = labelledMachineInstructions.containsKey(MachineInstructionLabel.CMOVE_64);
    var conditionalMove = getConditionalMove(hasCMove32, hasCMove64, labelledMachineInstructions);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, specification.simpleName());
    map.put("registerFiles", registerFiles);
    map.put("framePointer", framePointer);
    map.put("stackPointer", stackPointer);
    map.put("stackPointerByteSize", abi.stackPointer().registerFile().resultType().bitWidth() / 8);
    map.put("argumentRegisterClasses",
        abi.argumentRegisters().stream().map(Abi.RegisterRef::registerFile).distinct()
            .map(LlvmRegisterFile::new).toList());
    map.put("argumentRegisters", abi.argumentRegisters());
    map.put("stackPointerBitWidth", abi.stackPointer().registerFile().resultType().bitWidth());
    map.put("stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType());
    map.put("addressSequence", addressSequence);
    map.put("hasCMove32", hasCMove32);
    map.put("hasCMove64", hasCMove64);
    map.put("conditionalMove", conditionalMove);
    map.put("branchInstructions", getBranchInstructions(new Database(passResults, specification)));
    return map;
  }

  record BranchInstruction(String instructionName, String isdName) {

  }

  private List<BranchInstruction> getBranchInstructions(Database database) {
    var queryResult = database.run(new Query.Builder().machineInstructionLabelGroup(
        MachineInstructionLabelGroup.BRANCH_INSTRUCTIONS).build());
    var flipped = database.flipMachineInstructions();

    return queryResult.machineInstructions().stream().map(instruction -> {
      var machineInstructionLabel = ensureNonNull(flipped.get(instruction),
          () -> Diagnostic.error("Cannot find a label to the instruction",
              instruction.sourceLocation()));
      var condCode =
          ensureNonNull(MachineInstructionLabel.getLlvmCondCodeByLabel(machineInstructionLabel),
              () -> Diagnostic.error("There is no cond code for the machine instruction label.",
                  instruction.sourceLocation()));
      return new BranchInstruction(instruction.simpleName(), condCode.name());
    }).toList();
  }

  @Nullable
  private Instruction getConditionalMove(boolean hasCMove32,
                                         boolean hasCMove64,
                                         Map<MachineInstructionLabel,
                                             List<Instruction>> labelledMachineInstructions) {
    if (hasCMove64) {
      var cmove = labelledMachineInstructions.get(MachineInstructionLabel.CMOVE_32);
      ensureNonNull(cmove, "must not be null");
      return ensurePresent(cmove.stream().findFirst(), "At least one element should be present");
    } else if (hasCMove32) {
      var cmove = labelledMachineInstructions.get(MachineInstructionLabel.CMOVE_64);
      ensureNonNull(cmove, "must not be null");
      return ensurePresent(cmove.stream().findFirst(), "At least one element should be present");
    }

    return null;
  }
}
