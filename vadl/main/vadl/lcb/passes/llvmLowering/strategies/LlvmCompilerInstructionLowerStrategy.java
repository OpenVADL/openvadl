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

package vadl.lcb.passes.llvmLowering.strategies;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.gcb.passes.RegisterRef;
import vadl.gcb.passes.operands.CompilerInstructionOperandsCtx;
import vadl.gcb.passes.operands.GenerateInstructionOperandsPass;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.viam.CompilerInstruction;
import vadl.viam.ViamError;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Abstract class to lower {@link CompilerInstruction} to {@link LlvmLoweringRecord.Compiler}.
 */
public abstract class LlvmCompilerInstructionLowerStrategy {
  protected final List<LlvmInstructionLoweringStrategy> strategies;

  protected LlvmCompilerInstructionLowerStrategy(
      List<LlvmInstructionLoweringStrategy> strategies) {
    this.strategies = strategies;
  }

  /**
   * Lower an instruction.
   */
  public Optional<LlvmLoweringRecord.Compiler> lowerInstruction(
      CompilerInstruction compilerInstruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      DetermineRegisterUsesAndDefsPass.Info registerDefsUses
  ) {
    var uses = new ArrayList<RegisterRef>();
    var defs = new ArrayList<RegisterRef>();

    var isTerminator = false;
    var isReturn = false;
    var mayLoad = false;
    var mayStore = false;
    var isBranch = false;

    if (compilerInstruction.behavior().getNodes(InstrCallNode.class).limit(2).count() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning(
              "Cannot generate instruction selectors for pseudo instruction with multiple "
                  + "machine instructions",
              compilerInstruction.location()).build());
    }


    for (var callNode : compilerInstruction.behavior().getNodes(InstrCallNode.class).toList()) {
      var instruction = callNode.target();
      var instructionBehavior = instruction.behavior().copy();
      GenerateInstructionOperandsPass.replaceNodesInBehavior(instructionBehavior, callNode);

      var label = supportedInstructions.reverse().get(instruction);

      // Skip not supported instructions
      if (label == null) {
        continue;
      }

      for (var strategy : strategies) {
        if (!strategy.isApplicable(label)) {
          continue;
        }

        var baseInstructionInfo =
            strategy.lowerBaseInfo(instruction, instructionBehavior, registerDefsUses);

        var flags = baseInstructionInfo.flags();
        isTerminator |= flags.isTerminator();
        isReturn |= flags.isReturn();
        mayLoad |= flags.mayLoad();
        mayStore |= flags.mayStore();
        isBranch |= flags.isBranch();
        defs.addAll(baseInstructionInfo.defs());
        uses.addAll(baseInstructionInfo.uses());

        break;
      }
    }

    var flags = new LlvmLoweringPass.Flags(isTerminator,
        isBranch,
        false,
        isReturn,
        true,
        true,
        mayLoad,
        mayStore,
        false,
        false,
        false,
        false);

    var operandCtx =
        ViamError.ensureNonNull(compilerInstruction.extension(CompilerInstructionOperandsCtx.class),
            () -> Diagnostic.error("Cannot lookup operands for instruction",
                compilerInstruction.location()));

    var info = new LlvmLoweringPass.BaseInstructionInfo(
        operandCtx.inputs(),
        operandCtx.outputs(),
        flags,
        dedup(uses),
        dedup(defs)
    );

    return Optional.of(new LlvmLoweringRecord.Compiler(
        info
    ));
  }

  private <T> List<T> dedup(
      List<T> x) {
    return new ArrayList<>(new LinkedHashSet<>(x));
  }
}
