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

package vadl.pass.order;

import java.io.IOException;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.RenamingConflictingRegistersPass;
import vadl.lcb.passes.OverwriteInputOperandsPass;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.viam.passes.ArtificialResPartialAccessExpansionPass;
import vadl.viam.passes.ControlFlowOptimizationPass;
import vadl.viam.passes.DetectRegisterIndicesPass;
import vadl.viam.passes.DuplicateWriteDetectionPass;
import vadl.viam.passes.HardcodeLGALabelPass;
import vadl.viam.passes.InstructionResourceAccessAnalysisPass;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.behaviorRewrite.BehaviorRewritePass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.functionInliner.ArtificialResInlinerPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass;
import vadl.viam.passes.staticCounterAccess.CounterAccessResolvingPass;
import vadl.viam.passes.statusBuiltInInlinePass.RemoveUnusedStatusFlagsFromBuiltinsPass;
import vadl.viam.passes.statusBuiltInInlinePass.StatusBuiltInInlinePass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * Builds the shared VIAM pass order used by all backends.
 */
public final class ViamPassOrder {
  private ViamPassOrder() {
  }

  /**
   * Creates the pass order.
   */
  public static PassOrder create(GeneralConfiguration configuration) throws IOException {
    var order = new PassOrder();
    order.add(new PassOrders.ViamCreationPass(configuration));
    order.add(new ViamVerificationPass(configuration));

    order.add(new DetectRegisterIndicesPass(configuration));
    order.add(new OverwriteInputOperandsPass(configuration));
    order.add(new NormalizeFieldsToFieldAccessFunctionsPass(configuration));
    order.add(new RenamingConflictingRegistersPass(configuration));
    order.add(new SnapshotInstructionBehaviorPass(configuration));

    order.add(new RemoveUnusedStatusFlagsFromBuiltinsPass(configuration));
    order.add(new StatusBuiltInInlinePass(configuration));

    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));

    order.add(new CounterAccessResolvingPass(configuration));
    order.add(new FunctionInlinerPass(configuration));
    order.add(new FieldAccessInlinerPass(configuration));
    order.add(new ArtificialResPartialAccessExpansionPass(configuration));
    order.add(new ArtificialResInlinerPass(configuration));
    order.add(new ControlFlowOptimizationPass(configuration));
    order.add(new SideEffectConditionResolvingPass(configuration));
    order.add(new DuplicateWriteDetectionPass(configuration));

    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));
    order.add(new InstructionResourceAccessAnalysisPass(configuration));
    order.add(new HardcodeLGALabelPass(configuration));
    order.add(new ViamVerificationPass(configuration));
    return order;
  }
}
