// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassOrder;
import vadl.pass.PassResults;
import vadl.viam.Specification;
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
 * This class contains static methods that define the individual pass orders for different
 * generation targets (e.g., LCB, ISS, ...).
 */
public class PassOrders {

  /**
   * Used by the {@code check} command.
   * It doesn't apply transformation to the VIAM, however, it checks if the VDT can be constructed.
   */
  public static PassOrder check(GeneralConfiguration configuration) {
    var order = new PassOrder();
    order.add(new ViamCreationPass(configuration));

    PassOrderPipelineUtils.addHtmlDump(order, configuration, "VIAM Creation",
        "Dump directly after frontend generated VIAM.");

    order.add(new ViamVerificationPass(configuration));

    VdtPassOrders.addDecodePasses(order, configuration);

    PassOrderPipelineUtils.addHtmlDump(order, configuration,
        "VDT Creation",
        "Dump directly after VDT generation.");

    return order;
  }

  /**
   * Return the viam passes.
   */
  public static PassOrder viam(GeneralConfiguration configuration) throws IOException {
    var order = new PassOrder();

    // this is just a pseudo pass to add the behavior to the HTML dump
    // at the stage directly after the VIAM creation.
    order.add(new ViamCreationPass(configuration));
    order.add(new ViamVerificationPass(configuration));

    order.add(new DetectRegisterIndicesPass(configuration));
    order.add(new vadl.lcb.passes.OverwriteInputOperandsPass(configuration));
    order.add(new NormalizeFieldsToFieldAccessFunctionsPass(configuration));
    order.add(new vadl.gcb.passes.RenamingConflictingRegistersPass(configuration));
    order.add(new SnapshotInstructionBehaviorPass(configuration));

    order.add(new RemoveUnusedStatusFlagsFromBuiltinsPass(configuration));
    order.add(new StatusBuiltInInlinePass(configuration));

    // Common optimizations
    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));

    // TODO: @kper do you see any fix for this?
    // Note: we run the counter-access resolving pass before the func inliner pass
    // because the lcb uses the uninlined version of the instructions.
    // However, this might miss a lot of opportunities to statically resolve counter-accesses
    // as the canonicalization runs at a later point.
    order.add(new StaticCounterAccessResolvingPass(configuration));
    order.add(new FunctionInlinerPass(configuration));
    order.add(new FieldAccessInlinerPass(configuration));
    order.add(new ArtificialResInlinerPass(configuration));
    order.add(new ControlFlowOptimizationPass(configuration));
    order.add(new SideEffectConditionResolvingPass(configuration));
    // requires SideEffectConditionResolvingPass to work
    order.add(new DuplicateWriteDetectionPass(configuration));

    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));
    order.add(new InstructionResourceAccessAnalysisPass(configuration));

    // Hardcoded
    order.add(new HardcodeLGALabelPass(configuration));

    // verification after viam optimizations
    order.add(new ViamVerificationPass(configuration));

    return order;
  }

  /**
   * Return the gcb and cppcodegen passes.
   */
  public static PassOrder gcbAndCppCodeGen(GcbConfiguration gcbConfiguration) throws IOException {
    return LcbGcbPassOrders.extendGcbAndCppCodeGen(viam(gcbConfiguration), gcbConfiguration);
  }

  /**
   * This is the pass order which must be executed to get a LLVM compiler.
   */
  public static PassOrder lcb(LcbConfiguration configuration)
      throws IOException {
    return LcbGcbPassOrders.extendLcb(gcbAndCppCodeGen(configuration), configuration);
  }

  /**
   * Constructs the pass order used to generate the ISS (QEMU) from a VADL specification.
   */
  public static PassOrder iss(IssConfiguration config) throws IOException {
    return IssPassOrders.extendIss(viam(config), config);
  }

  /**
   * Constructs the pass order used to generate the RTL (Chisel) from a VADL specification.
   */
  public static PassOrder rtl(RtlConfiguration config) throws IOException {
    return RtlPassOrders.extendRtl(viam(config), config);
  }

  /**
   * A pseudo pass that indicates the first pass in the PassOrder.
   * It is necessary to dump the behavior directly after creation,
   * before any other pass manipulated the behavior.
   *
   * <p>This pass contains no logic.</p>
   */
  public static class ViamCreationPass extends Pass {

    public ViamCreationPass(GeneralConfiguration configuration) {
      super(configuration);
    }

    @Override
    public PassName getName() {
      return PassName.of("VIAM Creation (pseudo pass)");
    }

    @Nullable
    @Override
    public Object execute(PassResults passResults, Specification viam) throws IOException {
      return null;
    }
  }

}
