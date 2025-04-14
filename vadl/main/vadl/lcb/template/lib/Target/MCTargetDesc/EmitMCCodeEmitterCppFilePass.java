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

import static vadl.lcb.template.utils.ImmediateEncodingFunctionProvider.generateEncodeFunctions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.AutomaticallyGeneratedRelocation;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.utils.Triple;
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

  record Aggregate(String encodeWrapper, String encode) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "encodeWrapper", encodeWrapper,
          "encode", encode
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var immediates = generateImmediates(passResults);

    var symbolRefFixups = generateSymbolRefFixupMappings(passResults);
    var targetFixups = generateTargetFixupMappings(passResults);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "immediates", immediates,
        "symbolRefFixups", symbolRefFixups,
        "targetFixups", targetFixups
    );
  }


  private List<Aggregate> generateImmediates(PassResults passResults) {
    return generateEncodeFunctions(passResults)
        .values()
        .stream()
        .map(f -> new Aggregate(f.identifier.append(WRAPPER).lower(), f.identifier.lower()))
        .toList();
  }

  private List<Map<String, Object>> generateTargetFixupMappings(PassResults passResults) {

    var tableGenMachineInstructions =
        (List<TableGenMachineInstruction>) passResults.lastResultOf(
            GenerateTableGenMachineInstructionRecordPass.class);

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
        .filter(tableGenMachineInstruction -> {
          return !tableGenMachineInstruction.llvmLoweringRecord().info().inputImmediates()
              .isEmpty();
        })
        .map(tableGenMachineInstruction -> {
          var instruction = tableGenMachineInstruction.instruction();

          var immediateOperands =
              tableGenMachineInstruction.llvmLoweringRecord().info().inputs().stream()
                  .filter(i -> i instanceof ReferencesImmediateOperand)
                  .map(tableGenOperand -> {
                    var immediateOperand =
                        ((ReferencesImmediateOperand) tableGenOperand).immediateOperand();
                    var opIndex = tableGenMachineInstruction.indexInOperands(tableGenOperand);

                    // FIXME: find the correct fixup
                    //        currently always the ABS one is found for the field
                    var operanderFixup = linkerComponents.fixups().stream()
                        .filter(fixup ->
                            fixup.implementedRelocation()
                                instanceof AutomaticallyGeneratedRelocation relocation
                                && relocation.field()
                                .equals(immediateOperand.fieldAccessRef().fieldRef())
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
