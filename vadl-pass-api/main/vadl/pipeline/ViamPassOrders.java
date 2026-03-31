// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import java.io.IOException;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
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
import vadl.viam.passes.staticCounterAccess.StaticCounterAccessResolvingPass;
import vadl.viam.passes.statusBuiltInInlinePass.RemoveUnusedStatusFlagsFromBuiltinsPass;
import vadl.viam.passes.statusBuiltInInlinePass.StatusBuiltInInlinePass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * Shared VIAM pipeline fragments used as the base for backend-specific pipelines.
 */
public final class ViamPassOrders {
  private ViamPassOrders() {
  }

  /**
   * Creates the shared VIAM base pipeline used by backend-specific pipelines.
   */
  public static PassOrder viam(GeneralConfiguration configuration) throws IOException {
    var order = new PassOrder();

    order.add(new ViamCreationPass(configuration));
    order.add(new ViamVerificationPass(configuration));

    order.add(new DetectRegisterIndicesPass(configuration));
    order.add(new vadl.lcb.passes.OverwriteInputOperandsPass(configuration));
    order.add(new NormalizeFieldsToFieldAccessFunctionsPass(configuration));
    order.add(new vadl.gcb.passes.RenamingConflictingRegistersPass(configuration));
    order.add(new SnapshotInstructionBehaviorPass(configuration));

    order.add(new RemoveUnusedStatusFlagsFromBuiltinsPass(configuration));
    order.add(new StatusBuiltInInlinePass(configuration));

    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));

    order.add(new StaticCounterAccessResolvingPass(configuration));
    order.add(new FunctionInlinerPass(configuration));
    order.add(new FieldAccessInlinerPass(configuration));
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
