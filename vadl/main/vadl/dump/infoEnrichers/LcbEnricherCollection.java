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

package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionCtx;
import vadl.lcb.passes.isaMatching.PseudoInstructionCtx;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * A static collection {@link InfoEnricher} that provides
 * information about the LCB.
 */
public class LcbEnricherCollection {

  /**
   * Some instruction have a predefined behavior semantic.
   * It was detected by VADL then emit the label.
   */
  public static InfoEnricher ISA_MATCHING_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (definitionEntity, passResults) -> {
        // This supplier also runs for the VIAM dump.
        // But, the pass wasn't scheduled yet.
        if (!passResults.hasRunPassOnce(IsaMachineInstructionMatchingPass.class)) {
          return;
        }

        if (definitionEntity.origin() instanceof Instruction instruction) {
          if (instruction.hasExtension(MachineInstructionCtx.class)) {
            var label =
                Optional.ofNullable(
                        instruction.extension(
                            MachineInstructionCtx.class))
                    .map(MachineInstructionCtx::label)
                    .map(Enum::name).orElse("No label");
            var info = Info.Tag.of("Instruction Label", label);
            definitionEntity.addInfo(info);
          }
        }
      });


  /**
   * Some instruction have a predefined behavior semantic.
   * It was detected by VADL then emit the label.
   */
  public static InfoEnricher PSEUDO_ISA_MATCHING_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (definitionEntity, passResults) -> {
        // This supplier also runs for the VIAM dump.
        // But, the pass wasn't scheduled yet.
        if (!passResults.hasRunPassOnce(IsaPseudoInstructionMatchingPass.class)) {
          return;
        }

        if (definitionEntity.origin() instanceof PseudoInstruction instruction) {
          if (instruction.hasExtension(PseudoInstructionCtx.class)) {
            var label =
                Optional.ofNullable(
                        instruction.extension(
                            PseudoInstructionCtx.class))
                    .map(PseudoInstructionCtx::label)
                    .map(Enum::name).orElse("No label");
            var info = Info.Tag.of("Pseudo Instruction Label", label);
            definitionEntity.addInfo(info);
          }
        }
      });

  /**
   * Renders tablegen's instruction operands.
   */
  public static InfoEnricher LLVM_LOWERING_OPERANDS =
      forType(DefinitionEntity.class, (definitionEntity, passResults) -> {
        // This supplier also runs for the VIAM dump.
        // But, the pass wasn't scheduled yet.
        if (!passResults.hasRunPassOnce(LlvmLoweringPass.class)) {
          return;
        }

        var results =
            (LlvmLoweringPass.LlvmLoweringPassResult)
                passResults.lastResultOf(LlvmLoweringPass.class);

        if (results != null && definitionEntity.origin() instanceof Instruction instruction) {
          var result = (LlvmLoweringRecord) results.machineInstructionRecords().get(instruction);

          if (result != null) {
            var renderedInputOperands =
                result.info().inputs().stream().map(TableGenInstructionOperand::render).collect(
                    Collectors.joining(", "));
            var renderedOutputOperands =
                result.info().outputs().stream().map(TableGenInstructionOperand::render).collect(
                    Collectors.joining(", "));
            definitionEntity.addInfo(InfoUtils.createCodeBlockExpandable(
                "TableGen Input Operands",
                renderedInputOperands
            ));
            definitionEntity.addInfo(InfoUtils.createCodeBlockExpandable(
                "TableGen Output Operands",
                renderedOutputOperands
            ));
          }
        }
      });

  /**
   * A list of all info enrichers for the default lcb.
   */
  public static List<InfoEnricher> all = List.of(
      ISA_MATCHING_SUPPLIER_TAG,
      LLVM_LOWERING_OPERANDS
  );
}
