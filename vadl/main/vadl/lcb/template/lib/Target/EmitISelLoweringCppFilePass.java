// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.MachineInstructionLabelGroup;
import vadl.gcb.passes.ValueRange;
import vadl.gcb.passes.ValueRangeCtx;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.ISelLoweringOperationActionPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmMachineInstructionUtil;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.RegisterTensor;
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
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName + "ISelLowering.cpp";
  }

  static class LlvmRegisterFile extends RegisterTensor {

    /**
     * Constructs a new RegisterFile object.
     **/
    public LlvmRegisterFile(RegisterTensor registerFile) {
      super(registerFile.identifier,
          registerFile.dimensions());
      for (var c : registerFile.constraints()) {
        addConstraint(c);
      }
    }

    public String llvmResultType() {
      return ValueType.from(type()).get().getLlvmType();
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi = (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var registerFiles = ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
        GenerateTableGenRegistersPass.class)).registerClasses();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var absoluteAddressLoadInstruction = abi.absoluteAddressLoad();
    var labelledMachineInstructions = ensureNonNull(
        (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions",
            specification.location()))
        .labels();
    var coverageSummary =
        (ISelLoweringOperationActionPass.CoverageSummary) passResults.lastResultOf(
            ISelLoweringOperationActionPass.class);
    var hasCMove32 = labelledMachineInstructions.containsKey(MachineInstructionLabel.CMOVE_32);
    var hasCMove64 = labelledMachineInstructions.containsKey(MachineInstructionLabel.CMOVE_64);
    var conditionalMove = getConditionalMove(hasCMove32, hasCMove64, labelledMachineInstructions);
    var database = new Database(passResults, specification);
    var conditionalValueRange = getValueRangeCompareInstructions(database);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, lcbConfiguration().targetName().value().toLowerCase());
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
    map.put("absoluteAddressLoadInstruction",
        absoluteAddressLoadInstruction.identifier().simpleName());
    map.put("hasLocalAddressLoad", abi.localAddressLoad().isPresent());
    map.put("hasGlobalAddressLoad", abi.globalAddressLoad().isPresent());
    map.put("localAddressLoadInstruction",
        abi.localAddressLoad().map(x -> x.identifier().simpleName()).orElse(""));
    map.put("hasCMove32", hasCMove32);
    map.put("hasCMove64", hasCMove64);
    map.put("conditionalMove", conditionalMove);
    map.put("addImmediateInstruction", getAddImmediate(database));
    map.put("branchInstructions", getBranchInstructions(database));
    map.put("memoryInstructions", getMemoryInstructions(database));
    map.put("conditionalValueRangeLowest", conditionalValueRange.lowest());
    map.put("conditionalValueRangeHighest", conditionalValueRange.highest());
    map.put("expandableDagNodes", coverageSummary.notCoveredSelectionDagNodes());
    return map;
  }

  private ISelInstruction getAddImmediate(Database database) {
    var queryResult = database.run(
        new Query.Builder().machineInstructionLabel(MachineInstructionLabel.ADDI_64)
            .or(new Query.Builder().machineInstructionLabel(MachineInstructionLabel.ADDI_32)
                .build()).build());

    var instruction = queryResult.firstMachineInstruction();
    Supplier<DiagnosticBuilder> error =
        () -> Diagnostic.error("Addition-Register-Immediate requires a value range",
            instruction.location());
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

    var smallest = Long.MAX_VALUE;
    var highest = Long.MIN_VALUE;

    for (var instruction : queryResult.machineInstructions()) {
      var valueRangeCtx = instruction.extension(ValueRangeCtx.class);

      // The group `MachineInstructionLabelGroup.CONDITIONAL_INSTRUCTIONS` might also
      // have instructions without immediates. Therefore, it is ok that there is no value range.
      if (valueRangeCtx != null && !valueRangeCtx.ranges().isEmpty()) {
        var valueRange = ensurePresent(valueRangeCtx.getFirst(),
            () -> Diagnostic.error("Conditional instruction requires a value range",
                instruction.location()));

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
              instruction.location());

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
              instruction.location()));
      var condCode =
          ensureNonNull(LlvmMachineInstructionUtil.getLlvmCondCodeByLabel(machineInstructionLabel),
              () -> Diagnostic.error("There is no cond code for the machine instruction label.",
                  instruction.location()));
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
