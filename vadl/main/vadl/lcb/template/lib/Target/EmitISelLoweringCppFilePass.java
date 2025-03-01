package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticBuilder;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.ValueRange;
import vadl.gcb.passes.ValueRangeCtx;
import vadl.gcb.passes.relocation.model.Modifier;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.MachineInstructionLabelGroup;
import vadl.lcb.passes.isaMatching.RelocationFunctionLabel;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
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
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var linkerInformation = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var registerFiles = ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
        GenerateTableGenRegistersPass.class)).registerClasses();
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
    var database = new Database(passResults, specification);
    var addi = database.getAddImmediate();
    var conditionalValueRange = getValueRangeCompareInstructions(database);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, lcbConfiguration().processorName().value().toLowerCase());
    map.put("registerFiles", registerFiles.stream().map(this::mapRegisterFile).toList());
    map.put("framePointer", framePointer);
    map.put("stackPointer", stackPointer);
    map.put("stackPointerByteSize", abi.stackPointer().registerFile().resultType().bitWidth() / 8);
    map.put("argumentRegisterClasses",
        abi.argumentRegisters().stream().map(Abi.RegisterRef::registerFile).distinct()
            .map(LlvmRegisterFile::new).map(this::mapLlvmRegisterClass).toList());
    map.put("argumentRegisters",
        abi.argumentRegisters().stream().map(Abi.RegisterRef::render).toList());
    map.put("stackPointerBitWidth", abi.stackPointer().registerFile().resultType().bitWidth());
    map.put("stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType());
    map.put("addressSequence", addressSequence.simpleName());
    map.put("hasCMove32", hasCMove32);
    map.put("hasCMove64", hasCMove64);
    map.put("conditionalMove", conditionalMove);
    map.put("addImmediateInstruction", getAddImmediate(database));
    map.put("branchInstructions", getBranchInstructions(database));
    map.put("memoryInstructions", getMemoryInstructions(database));
    map.put("conditionalValueRangeLowest", conditionalValueRange.lowest());
    map.put("conditionalValueRangeHighest", conditionalValueRange.highest());
    map.put("addImmediateHighModifier",
        findHighModifier(addi, linkerInformation, fieldUsages).value());
    map.put("addImmediateLowModifier",
        findLowModifier(addi, linkerInformation, fieldUsages).value());
    return map;
  }

  private Modifier findHighModifier(Instruction instruction,
                                    GenerateLinkerComponentsPass.Output output,
                                    IdentifyFieldUsagePass.ImmediateDetectionContainer
                                        fieldUsages) {
    return findModifier(instruction, output, fieldUsages, RelocationFunctionLabel.HI);
  }

  private Modifier findLowModifier(Instruction instruction,
                                   GenerateLinkerComponentsPass.Output output,
                                   IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages) {
    return findModifier(instruction, output, fieldUsages, RelocationFunctionLabel.LO);
  }

  private Modifier findModifier(Instruction instruction,
                                GenerateLinkerComponentsPass.Output output,
                                IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
                                RelocationFunctionLabel relocationFunctionLabel) {
    var immediate =
        ensurePresent(fieldUsages.getImmediates(instruction.format()).stream().findFirst(),
            () -> Diagnostic.error("Expected to have an immediate", instruction.sourceLocation()));

    var modifiers = output.modifiers().stream()
        .filter(x -> x.kind().isAbsolute())
        .filter(x -> x.field().equals(immediate))
        .filter(x -> x.relocationFunctionLabel().isPresent()
            && x.relocationFunctionLabel().get() == relocationFunctionLabel)
        .toList();

    return ensurePresent(modifiers.stream().findFirst(),
        () -> Diagnostic.error("Cannot find a modifier for the instruction.",
            instruction.sourceLocation()));
  }

  private ISelInstruction getAddImmediate(Database database) {
    var queryResult = database.run(
        new Query.Builder().machineInstructionLabel(MachineInstructionLabel.ADDI_64)
            .or(new Query.Builder().machineInstructionLabel(MachineInstructionLabel.ADDI_32)
                .build()).build());

    var instruction = queryResult.firstMachineInstruction();
    Supplier<DiagnosticBuilder> error =
        () -> Diagnostic.error("Addition-Register-Immediate requires a value range",
            instruction.sourceLocation());
    var valueRangeCtx = ensureNonNull(instruction.extension(ValueRangeCtx.class), error);
    var valueRange = ensurePresent(valueRangeCtx.getFirst(), error);

    return new ISelInstruction(instruction.simpleName(), valueRange);
  }

  /**
   * LLVM needs a method to check whether an immediate fits into a conditional instruction.
   * However, it does not provide an instruction. Therefore, this must be the smallest/highest
   * range across all compares.
   */
  private ValueRange getValueRangeCompareInstructions(Database database) {
    var queryResult = database.run(
        new Query.Builder().machineInstructionLabelGroup(
            MachineInstructionLabelGroup.CONDITIONAL_INSTRUCTIONS).build());

    var smallest = Integer.MAX_VALUE;
    var highest = Integer.MIN_VALUE;

    for (var instruction : queryResult.machineInstructions()) {
      var valueRangeCtx = instruction.extension(ValueRangeCtx.class);

      // The group `MachineInstructionLabelGroup.CONDITIONAL_INSTRUCTIONS` might also
      // have instructions without immediates. Therefore, it is ok that there is no value range.
      if (valueRangeCtx != null && !valueRangeCtx.ranges().isEmpty()) {
        var valueRange = ensurePresent(valueRangeCtx.getFirst(),
            () -> Diagnostic.error("Conditional instruction requires a value range",
                instruction.sourceLocation()));

        if (valueRange.lowest() < smallest) {
          smallest = valueRange.lowest();
        }

        if (valueRange.highest() > highest) {
          highest = valueRange.highest();
        }
      }
    }

    return new ValueRange(smallest, highest);
  }

  record BranchInstruction(String instructionName, String isdName) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "instructionName", instructionName,
          "isdName", isdName
      );
    }
  }

  record ISelInstruction(String instructionName, ValueRange offsetValueRange)
      implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "instructionName", instructionName,
          "minValue", offsetValueRange.lowest(),
          "maxValue", offsetValueRange.highest()
      );
    }
  }

  private List<ISelInstruction> getMemoryInstructions(Database database) {
    var queryResult = database.run(new Query.Builder().machineInstructionLabelGroup(
        MachineInstructionLabelGroup.MEMORY_INSTRUCTIONS).build());
    return queryResult.machineInstructions().stream().map(instruction -> {
      Supplier<DiagnosticBuilder> error =
          () -> Diagnostic.error("Memory instruction requires a value range",
              instruction.sourceLocation());

      var ctx = ensureNonNull(instruction.extension(ValueRangeCtx.class), error);
      var valueRange = ensurePresent(ctx.getFirst(), error);

      return new ISelInstruction(instruction.simpleName(), valueRange);
    }).toList();
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

  private Map<String, Object> mapLlvmRegisterClass(LlvmRegisterFile registerFile) {
    return Map.of(
        "name", registerFile.simpleName(),
        "resultWidth", registerFile.resultType().bitWidth(),
        "llvmResultType", registerFile.llvmResultType()
    );
  }

  private Map<String, Object> mapRegisterFile(TableGenRegisterClass registerFile) {
    return Map.of(
        "name", registerFile.name(),
        "regTypes", registerFile.regTypes(),
        "registerFileRef", Map.of(
            "name", registerFile.registerFileRef().simpleName()
        )
    );
  }
}
