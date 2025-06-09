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

package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.GcbCppEncodingWrapperFunction;
import vadl.gcb.passes.RelocationKindCtx;
import vadl.gcb.passes.relocation.model.AutomaticallyGeneratedRelocation;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateEncodingFunctionProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.utils.Triple;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * This file contains the logic for emitting MC instructions.
 */
public class EmitMCCodeEmitterCppFilePass extends LcbTemplateRenderingPass {

  public EmitMCCodeEmitterCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCCodeEmitter.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCCodeEmitter.cpp";
  }

  /**
   * The LLVM's encoder/decoder does not interact with the {@code uint64_t decode(uint64_t)}
   * functions but with {@code unsigned decode(const MCInst InstMI, ...} from the MCCodeEmitter.
   * This {@code WRAPPER} is just the magic suffix for the
   * function.
   */
  public static final String WRAPPER = "wrapper";

  record OperandPosition(String fieldAccessName, int opIndex) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "fieldAccessName", fieldAccessName,
          "opIndex", opIndex
      );
    }
  }

  record Aggregate(String encodeWrapper, List<OperandPosition> operands)
      implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "encodeWrapper", encodeWrapper,
          //"encode", encode,
          "operands", operands
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var tableGenMachineInstructions =
        (List<TableGenMachineInstruction>) passResults.lastResultOf(
            GenerateTableGenMachineInstructionRecordPass.class);
    var tableGenMachineInstructionsByInstruction = tableGenMachineInstructions
        .stream()
        .collect(Collectors.toMap(TableGenMachineInstruction::instruction, x -> x));
    var functions = (CreateFunctionsFromImmediatesPass.Output) passResults.lastResultOf(
        CreateFunctionsFromImmediatesPass.class);
    var encodingWrappers =
        generateWrapperEncodings(tableGenMachineInstructionsByInstruction, functions, passResults);

    var symbolRefFixups = generateSymbolRefFixupMappings(passResults);
    var targetFixups = generateTargetFixupMappings(tableGenMachineInstructions, passResults);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "encodings", encodingWrappers,
        "symbolRefFixups", symbolRefFixups,
        "targetFixups", targetFixups
    );
  }


  private List<Aggregate> generateWrapperEncodings(
      Map<Instruction, TableGenMachineInstruction> machineInstructions,
      CreateFunctionsFromImmediatesPass.Output functions,
      PassResults passResults) {
    return ImmediateEncodingFunctionProvider.generateEncodeWrapperFunctions(passResults)
        .keySet()
        .stream()
        .map(instruction -> {
          var tableGenInstruction = Objects.requireNonNull(machineInstructions.get(instruction));
          var encodingWrapper =
              Objects.requireNonNull(functions.encodingsWrappers().get(instruction));
          var operands =
              operandPositions(tableGenInstruction, encodingWrapper);

          return new Aggregate(encodingWrapper.identifier.lower(),
              //immediateRecord.rawEncoderMethod().lower(),
              operands);
        })
        .toList();
  }

  private List<OperandPosition> operandPositions(
      TableGenMachineInstruction tableGenInstruction,
      GcbCppEncodingWrapperFunction wrapperFunction) {
    var fieldAccesses = wrapperFunction.fieldAccesses().stream().toList();

    return fieldAccesses.stream()
        .map(fieldAccess -> new OperandPosition(fieldAccess.simpleName(),
            tableGenInstruction.indexInOperands(fieldAccess)))
        .toList();
  }

  private List<Map<String, Object>> generateTargetFixupMappings(
      List<TableGenMachineInstruction> tableGenMachineInstructions, PassResults passResults) {
    var linkerComponents = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);

    return linkerComponents.variantKindStore().relocationVariantKinds().entrySet().stream().map(
        entry -> {
          var relocationBeforeExpand = entry.getKey();
          var variantKind = entry.getValue();

          var userSpecifiedRelocations =
              linkerComponents.elfRelocations().stream().filter(
                      userReloc -> userReloc.relocation() == relocationBeforeExpand.relocation())
                  .toList();

          var opIndexFixupMap = new ArrayList<Triple<String, Integer, String>>();

          tableGenMachineInstructions.stream()
              .filter(tableGenMachineInstruction -> !tableGenMachineInstruction.llvmLoweringRecord()
                  .info().inputImmediates().isEmpty()
              ).forEach(
                  tableGenMachineInstruction -> {
                    tableGenMachineInstruction.llvmLoweringRecord().info().inputImmediates()
                        .forEach(
                            immOp -> {
                              var field = immOp.immediateOperand().fieldAccessRef().fieldRef();

                              var info = tableGenMachineInstruction.llvmLoweringRecord().info();
                              var opIndex = info.findInputIndex(field) + info.outputs().size();
                              var loweredRelocationsForRelocationWithThisOperand =
                                  userSpecifiedRelocations.stream().filter(
                                      userReloc -> userReloc.field().equals(field));

                              loweredRelocationsForRelocationWithThisOperand.forEach(
                                  loweredReloc ->
                                      opIndexFixupMap.add(
                                          new Triple<>(tableGenMachineInstruction.getName(),
                                              opIndex, loweredReloc.fixup().name().value()))

                              );
                            }
                        );
                  }
              );

          return Map.of(
              "variantKind", variantKind.value(),
              "instructionOperands", opIndexFixupMap.stream().map(
                  opFixups -> Map.of(
                      "instruction", opFixups.left(),
                      "opIndex", opFixups.middle(),
                      "fixup", opFixups.right()
                  )).toList()
          );
        }
    ).toList();
  }

  private List<Map<String, Object>> generateSymbolRefFixupMappings(
      PassResults passResults) {
    var tableGenMachineInstructions =
        (List<TableGenMachineInstruction>) passResults.lastResultOf(
            GenerateTableGenMachineInstructionRecordPass.class);

    var linkerComponents = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);

    return tableGenMachineInstructions.stream()
        .filter(
            tableGenMachineInstruction -> !tableGenMachineInstruction.llvmLoweringRecord().info()
                .inputImmediates()
                .isEmpty())
        .map(tableGenMachineInstruction -> {
          var instruction = tableGenMachineInstruction.instruction();

          var immediateOperands =
              tableGenMachineInstruction.llvmLoweringRecord().info().inputs().stream()
                  .filter(i -> i instanceof ReferencesImmediateOperand)
                  .map(tableGenOperand -> {
                    var immediateOperand =
                        ((ReferencesImmediateOperand) tableGenOperand).immediateOperand();
                    var opIndex = tableGenMachineInstruction.indexInOperands(tableGenOperand);

                    var relocationKindExtension = ensureNonNull(
                        instruction.extension(RelocationKindCtx.class), "must not be null");
                    var relocationKind = relocationKindExtension.getFieldToKind()
                        .get(immediateOperand.fieldAccessRef().fieldRef());

                    var operanderFixup = linkerComponents.fixups().stream()
                        .filter(fixup ->
                            fixup.implementedRelocation()
                                instanceof AutomaticallyGeneratedRelocation relocation
                                && relocation.field()
                                .equals(immediateOperand.fieldAccessRef().fieldRef())
                                && relocation.kind() == relocationKind
                        ).findFirst().orElseThrow();

                    return Map.of(
                        "opIndex", opIndex,
                        "fixup", operanderFixup.name().value()
                    );
                  }).toList();

          return Map.of(
              "instruction", instruction.simpleName(),
              "immediateOperands", immediateOperands
          );
        }).toList();
  }
}
