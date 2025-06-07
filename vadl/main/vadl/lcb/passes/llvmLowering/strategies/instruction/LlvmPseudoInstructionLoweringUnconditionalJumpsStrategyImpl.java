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

package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.llvmLowering.strategies.LoweringStrategyUtils.replaceBasicBlockByLabelImmediateInMachineInstruction;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.PseudoInstructionLabel;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionBareSymbolOperand;
import vadl.viam.Abi;
import vadl.viam.Format;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Lowers unconditional jumps into TableGen.
 */
public class LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl extends
    LlvmPseudoInstructionLowerStrategy {
  /**
   * Constructor.
   */
  public LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }

  @Override
  protected Set<PseudoInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(PseudoInstructionLabel.J);
  }

  @Override
  public Optional<LlvmLoweringRecord.Pseudo> lowerInstruction(
      Abi abi,
      List<TableGenInstAlias> instAliases,
      PseudoInstruction pseudo,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions) {
    for (var callNode : pseudo.behavior().getNodes(InstrCallNode.class).toList()) {
      var instructionBehavior = callNode.target().behavior().copy();
      var label = labelledMachineInstructions.reverse().get(callNode.target());
      if (label == null) {
        continue;
      }

      for (var strategy : strategies) {
        if (!strategy.isApplicable(label)) {
          continue;
        }

        var tableGenRecord =
            strategy.lowerInstruction(labelledMachineInstructions, callNode.target(),
                instructionBehavior,
                abi);

        if (tableGenRecord.isPresent()) {
          var record = tableGenRecord.get();
          /* The unconditional jump requires an immediate which is the basic block which
           we should jump to. However, the pseudo instruction's machine instruction has no immediate
           because it is replaced by a `PseudoFuncParam`.

           pseudo instruction J( offset : Bits<20> ) =
         {
            JAL{ rd = 0 as Bits5, imm = offset }
         }

         Here, for example, you have a pseudo instruction where the `imm` is set with `offset` which
         is not an immediate.
         We solve this problem by looking at `JAL`'s format and seeing that there is only one field
         access function. This approach will only work when there is *only* one field access
         function.
         Furthermore, the solution has two parts: generating patterns and generating the input
         operands. The pattern part is covered in `generatePatternVariations`. The input operand
         is overwritten here.
         */

          var instrCallNode = getInstrCallNodeOrThrowError(pseudo);
          var fieldAccess = getFieldAccessFunctionFromFormatOrThrowError(instrCallNode);
          var fieldAccessNode = new LlvmFieldAccessRefNode(pseudo,
              fieldAccess,
              fieldAccess.type(),
              upcastFieldAccess(fieldAccess),
              LlvmFieldAccessRefNode.Usage.BasicBlock);
          var inputOperand =
              LlvmInstructionLoweringStrategy.generateTableGenInputOutput(fieldAccessNode);
          var flags =
              LlvmLoweringPass.Flags.withPseudo(
                  LlvmLoweringPass.Flags.withBarrier(LlvmLoweringPass.Flags.withBranch(
                      LlvmLoweringPass.Flags.withTerminator(tableGenRecord.get().info().flags()))
                  ));

          // Overwrite input, output operands and flags.
          var newInfo = new LlvmLoweringPass.BaseInstructionInfo(
              List.of(inputOperand),
              Collections.emptyList(),
              flags,
              tableGenRecord.get().info().uses(),
              tableGenRecord.get().info().defs());
          return Optional.of(
              new LlvmLoweringRecord.Pseudo(newInfo,
                  generatePatternVariations(pseudo, record),
                  instAliases));
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      PseudoInstruction pseudo,
      LlvmLoweringRecord record) {
    /*
    def : Pat<(br bb:$offset),
          (J RV32I_Jtype_ImmediateJ_immediateAsLabel:$offset)>;
     */
    var selector = new Graph("selector");
    var machine = new Graph("selector");

    var instrCallNode = getInstrCallNodeOrThrowError(pseudo);
    var fieldAccess = getFieldAccessFunctionFromFormatOrThrowError(instrCallNode);
    var upcasted = upcastFieldAccess(fieldAccess);

    selector.addWithInputs(
        new LlvmBrSD(new LlvmBasicBlockSD(
            pseudo,
            fieldAccess,
            fieldAccess.type(),
            upcasted)));
    machine.addWithInputs(new LcbPseudoInstructionNode(
        new NodeList<>(
            new LcbMachineInstructionParameterNode(new TableGenInstructionBareSymbolOperand(
                new LlvmBasicBlockSD(pseudo, fieldAccess, fieldAccess.type(), upcasted),
                fieldAccess.simpleName()))
        ), pseudo));

    return List.of(
        replaceBasicBlockByLabelImmediateInMachineInstruction(
            new TableGenSelectionWithOutputPattern(selector, machine)
        )
    );
  }

  private static @Nonnull ValueType upcastFieldAccess(Format.FieldAccess fieldAccess) {
    return ensurePresent(ValueType.from(CppTypeMap.upcast(fieldAccess.type())),
        () -> Diagnostic.error(
                String.format("Cannot convert immediate type to LLVM type: %s", fieldAccess.type()),
                fieldAccess.location())
            .help("Check whether this type exists in LLVM"));
  }

  private static @Nonnull InstrCallNode getInstrCallNodeOrThrowError(PseudoInstruction pseudo) {
    ensure(pseudo.behavior().getNodes(InstrCallNode.class).count() == 1,
        () -> Diagnostic.error("Expected only one machine instruction",
            pseudo.location()));
    return
        ensurePresent(pseudo.behavior().getNodes(InstrCallNode.class).findFirst(),
            () -> Diagnostic.error("Expected only one machine instruction",
                pseudo.location()));
  }

  private static @Nonnull Format.FieldAccess getFieldAccessFunctionFromFormatOrThrowError(
      InstrCallNode machineInstruction) {
    var usedFieldAccessFunctions = machineInstruction.usedFieldAccesses();
    if (usedFieldAccessFunctions.isEmpty()) {
      throw Diagnostic.error(
              "Machine instruction must have one field access function to be able to "
                  + "deduce the immediate layout for the machine instruction.",
              machineInstruction.location())
          .help("Use a field access function so the generator knows that this is the immediate.")
          .build();
    } else if (usedFieldAccessFunctions.size() > 1) {
      throw Diagnostic.error(
          "Machine instruction must only have exactly one field access function to be able to "
              + "deduce the immediate layout for the machine instruction.",
          machineInstruction.location()).build();
    }

    return
        ensurePresent(machineInstruction.target().format().fieldAccesses().stream().findFirst(),
            () -> Diagnostic.error("Cannot find a field access function",
                machineInstruction.location()));
  }
}
