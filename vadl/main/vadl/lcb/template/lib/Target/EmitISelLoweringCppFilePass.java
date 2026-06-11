// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticBuilder;
import vadl.gcb.passes.GenerateGcbIntrinsicsPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.MachineInstructionLabelGroup;
import vadl.gcb.passes.ValueRange;
import vadl.gcb.passes.ValueRangeCtx;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.isaMatching.database.QueryResult;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.ISelLoweringOperationActionPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmMachineInstructionUtil;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.RegisterResource;
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
    public LlvmRegisterFile(RegisterResource registerFile) {
      super(registerFile.identifier(),
          registerFile.dimensions());
      for (var c : registerFile.constraints()) {
        addConstraint(c);
      }
    }

    public String llvmResultType() {
      return ValueType.from(type()).get().getLlvmType();
    }
  }

  /**
   * When the backend has multiple register files with different types, we need a way to change the
   * type. For example, AArch64 has {@code X} and {@code W} which is essentially the same but
   * different register classes in LLVM. A {@link TruncationCustomization} is the C++ code to
   * truncate the value from {@link ValueType#I64} to {@link ValueType#I32}.
   *
   * @param src    is the source type.
   * @param dest   is the destination type.
   * @param subIdx is the name of the sub register index, defined in {@code RegisterInfo.td}.
   */
  record TruncationCustomization(ValueType src, ValueType dest, String subIdx)
      implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("src", src.getLlvmType(), "dest", dest.getLlvmType(), "subIdx", subIdx);
    }
  }

  /**
   * Tablegen cannot map intrinsics that have no inputs and outputs. That's why they have to be
   * lowered in the ISelLowering. We add a mapping to map them into machine nodes (but only for
   * those intrinsics).
   *
   * @param intrinsicName   is the name of the intrinsic.
   * @param instructionName is the name of the instruction that has to be emitted.
   */
  record ZeroOutputInputIntrinsic(String intrinsicName, String instructionName)
      implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("intrinsicName", intrinsicName,
          "instructionName", instructionName);
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi = (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var generateTableGenRegistersPassOutput =
        ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
            GenerateTableGenRegistersPass.class));
    var registerFiles =
        Stream.concat(generateTableGenRegistersPassOutput.registerClasses().stream(),
            generateTableGenRegistersPassOutput.aliasRegisterClasses().stream()).toList();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var absoluteAddressLoadInstruction = abi.absoluteAddressLoad();
    var coverageSummary =
        (ISelLoweringOperationActionPass.CoverageSummary) passResults.lastResultOf(
            ISelLoweringOperationActionPass.class);
    var database = new Database(passResults, specification);
    var conditionalValueRange = getValueRangeCompareInstructions(database);
    var stackPointerType =
        ValueType.from(abi.stackPointer().registerFile().resultType()).get();

    var intrinsicOutput =
        (GenerateGcbIntrinsicsPass.Output) passResults
            .lastResultOf(
                GenerateGcbIntrinsicsPass.class);
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, lcbConfiguration().targetName().value().toLowerCase());
    map.put("registerFiles", registerFiles);
    map.put("mainRegisterFile",
        registerFiles.stream().filter(x -> x.regTypes().get(0).equals(stackPointerType)).findFirst()
            .get());
    map.put("framePointer", framePointer);
    map.put("stackPointer", stackPointer);
    map.put("stackPointerByteSize", abi.stackPointer().registerFile().resultType().bitWidth() / 8);
    map.put("argumentRegisterClasses",
        abi.argumentRegisters().stream().map(Abi.AbiRegister::registerFile).distinct()
            .map(LlvmRegisterFile::new).map(this::mapLlvmRegisterClass).toList());
    map.put("argumentRegisters",
        abi.argumentRegisters().stream().map(this::renderRegister).toList());
    map.put("stackPointerBitWidth", abi.stackPointer().registerFile().resultType().bitWidth());
    map.put("stackPointerType", stackPointerType.getLlvmType());
    map.put("absoluteAddressLoadInstruction",
        absoluteAddressLoadInstruction.identifier().simpleName());
    map.put("hasLocalAddressLoad", abi.localAddressLoad().isPresent());
    map.put("hasGlobalAddressLoad", abi.globalAddressLoad().isPresent());
    map.put("localAddressLoadInstruction",
        abi.localAddressLoad().map(x -> x.identifier().simpleName()).orElse(""));
    map.put("addImmediateInstruction", getAddImmediate(database));
    map.put("branchInstructions", getBranchInstructions(database));
    map.put("memoryInstructions", getMemoryInstructions(database));
    map.put("conditionalValueRangeLowest", conditionalValueRange.lowest());
    map.put("conditionalValueRangeHighest", conditionalValueRange.highest());
    map.put("expandableDagNodes", coverageSummary.notCoveredSelectionDagNodes());
    map.put("branchTypes", branchTypes(stackPointerType));
    map.put("mergedCmpAndBranch",
        !database.run(
            new Query.Builder().machineInstructionLabels(List.of(
                    MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_32,
                    MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_64))
                .build()).machineInstructions().isEmpty()
    );
    map.put("SUBS", getFirstNameOrEmpty(database.run(
        new Query.Builder().machineInstructionLabel(
                stackPointerType == ValueType.I32
                    ? MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_32 :
                    MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_64)
            .build())));
    map.put("B_ULT", getBranchInstructionNameOrEmpty(database, 
          MachineInstructionLabel.BULTH_BY_STATUS_REGISTER));
    map.put("B_LT", getBranchInstructionNameOrEmpty(database, 
          MachineInstructionLabel.BSLTH_BY_STATUS_REGISTER));
    map.put("B_EQ", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BEQ_BY_STATUS_REGISTER));
    map.put("B_NEQ", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BNEQ_BY_STATUS_REGISTER));
    map.put("B_ULE", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BULEQ_BY_STATUS_REGISTER));
    map.put("B_LE", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BSLEQ_BY_STATUS_REGISTER));
    map.put("B_UGT", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BUGTH_BY_STATUS_REGISTER));
    map.put("B_GT", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BSGTH_BY_STATUS_REGISTER));
    map.put("B_UGE", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BUGEQ_BY_STATUS_REGISTER));
    map.put("B_GE", getBranchInstructionNameOrEmpty(database,
          MachineInstructionLabel.BSGEQ_BY_STATUS_REGISTER));
    map.put("CSEL_SGEQ", getCselInstructionNameOrEmpty(database, stackPointerType, 
          MachineInstructionLabel.CSEL_SGEQ_I32, 
          MachineInstructionLabel.CSEL_SGEQ_I64));
    map.put("CSEL_UGEQ", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_UGEQ_I32,
          MachineInstructionLabel.CSEL_UGEQ_I64));
    map.put("CSEL_SLEQ", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_SLEQ_I32,
          MachineInstructionLabel.CSEL_SLEQ_I64));
    map.put("CSEL_ULEQ", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_ULEQ_I32,
          MachineInstructionLabel.CSEL_ULEQ_I64));
    map.put("CSEL_SLTH", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_SLTH_I32,
          MachineInstructionLabel.CSEL_SLTH_I64));
    map.put("CSEL_ULTH", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_ULTH_I32,
          MachineInstructionLabel.CSEL_ULTH_I64));
    map.put("CSEL_SGTH", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_SGTH_I32,
          MachineInstructionLabel.CSEL_SGTH_I64));
    map.put("CSEL_UGTH", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_UGTH_I32,
          MachineInstructionLabel.CSEL_UGTH_I64));
    map.put("CSEL_EQ", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_EQ_I32,
          MachineInstructionLabel.CSEL_EQ_I64));
    map.put("CSEL_NEQ", getCselInstructionNameOrEmpty(database, stackPointerType,
          MachineInstructionLabel.CSEL_NEQ_I32,
          MachineInstructionLabel.CSEL_NEQ_I64));
    map.put("zeroOutputInputIntrinsics",
        zeroOutputInputIntrinsics(intrinsicOutput, tableGenMachineRecords));
    return map;
  }

  private String getBranchInstructionNameOrEmpty(
      Database database, 
      MachineInstructionLabel label) {
    return getFirstNameOrEmpty(database.run(
          new Query.Builder().machineInstructionLabel(label).build()));
  }

  private String getCselInstructionNameOrEmpty(
      Database database, 
      ValueType stackPointerType, 
      MachineInstructionLabel label32, 
      MachineInstructionLabel label64) {
    return getFirstNameOrEmpty(database.run(
        new Query.Builder().machineInstructionLabel(
                stackPointerType == ValueType.I32
                    ? label32 
                    : label64)
            .build()));
  }

  private List<ZeroOutputInputIntrinsic> zeroOutputInputIntrinsics(
      GenerateGcbIntrinsicsPass.Output intrinsicOutput,
      List<TableGenMachineInstruction> tableGenMachineRecords) {
    var records = tableGenMachineRecords.stream().collect(Collectors.toMap(
        TableGenMachineInstruction::instruction, x -> x));
    return intrinsicOutput.intrinsics().stream()
        .filter(intrinsic -> {
          var tableGenRecord = records.get(intrinsic.instruction());

          if (tableGenRecord != null) {
            return tableGenRecord.getInOperands().isEmpty() && tableGenRecord.getOutOperands()
                .isEmpty();
          }

          return false;
        })
        .map(intrinsic -> new ZeroOutputInputIntrinsic(intrinsic.intrinsicName(),
            intrinsic.instruction().simpleName()))
        .toList();
  }

  private String branchTypes(ValueType stackPointerType) {
    if (stackPointerType == ValueType.I64) {
      return "MVT::i32, MVT::i64";
    } else {
      return "MVT::i32";
    }
  }
  
  private String getFirstNameOrEmpty(QueryResult result) {
    return result.machineInstructions().stream().map(x -> x.identifier().simpleName()).findFirst()
        .orElse("");
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
    return queryResult.machineInstructions().stream()
        .map(instruction -> {
          Supplier<DiagnosticBuilder> error =
              () -> Diagnostic.error("Memory instruction requires a value range",
                  instruction.location());

          var ctx = ensureNonNull(instruction.extension(ValueRangeCtx.class), error);
          var valueRange = ensurePresent(ctx.getFirst(), error);

          return new ISelInstruction(instruction.simpleName(), valueRange);
        })
        .sorted(Comparator.comparing(ISelInstruction::instructionName))
        .toList();
  }

  private List<BranchInstruction> getBranchInstructions(Database database) {
    var queryResult = database.run(new Query.Builder().machineInstructionLabelGroup(
        MachineInstructionLabelGroup.BRANCH_INSTRUCTIONS).build());
    var flipped = database.flipMachineInstructions();

    return queryResult.machineInstructions().stream()
        .map(instruction -> {
          var machineInstructionLabel = ensureNonNull(flipped.get(instruction),
              () -> Diagnostic.error("Cannot find a label to the instruction",
                  instruction.location()));
          var condCode =
              ensureNonNull(
                  LlvmMachineInstructionUtil.getLlvmCondCodeByLabel(machineInstructionLabel),
                  () -> Diagnostic.error("There is no cond code for the machine instruction label.",
                      instruction.location()));
          return new BranchInstruction(instruction.simpleName(), condCode.name());
        })
        .sorted(Comparator.comparing(BranchInstruction::instructionName))
        .toList();
  }

  private Map<String, Object> mapLlvmRegisterClass(LlvmRegisterFile registerFile) {
    return Map.of(
        "name", registerFile.simpleName(),
        "resultWidth", registerFile.resultType().bitWidth(),
        "llvmResultType", registerFile.llvmResultType()
    );
  }
}
