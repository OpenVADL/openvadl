// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import static vadl.configuration.DecoderOptions.Generator.RTL_TABLE;

import vadl.configuration.RtlConfiguration;
import vadl.lcb.passes.OverwriteInputOperandsPass;
import vadl.pass.PassOrder;
import vadl.rtl.passes.CleanupEmitDirectoryPass;
import vadl.rtl.passes.ControlLogicPass;
import vadl.rtl.passes.DebugOutputPass;
import vadl.rtl.passes.EmitBuildSbtPass;
import vadl.rtl.passes.EmitCoreEmitPass;
import vadl.rtl.passes.EmitCoreTestPass;
import vadl.rtl.passes.EmitElfSimPass;
import vadl.rtl.passes.EmitModulesPass;
import vadl.rtl.passes.EmitRVFIOutputsPass;
import vadl.rtl.passes.EmitRtlDevcontainerConfigPass;
import vadl.rtl.passes.EmitRtlDevcontainerDockerComposePass;
import vadl.rtl.passes.EmitRtlMakefilePass;
import vadl.rtl.passes.EmitScalafmtConfigPass;
import vadl.rtl.passes.EmitSimMemCppPass;
import vadl.rtl.passes.EmitSimMemSvPass;
import vadl.rtl.passes.EmitVadlLibPass;
import vadl.rtl.passes.ForwardingLogicPass;
import vadl.rtl.passes.HazardAnalysisPass;
import vadl.rtl.passes.InstructionProgressGraphCreationPass;
import vadl.rtl.passes.InstructionProgressGraphLowerPass;
import vadl.rtl.passes.InstructionProgressGraphMergePass;
import vadl.rtl.passes.InstructionProgressGraphNamePass;
import vadl.rtl.passes.MiaMappingCreationPass;
import vadl.rtl.passes.MiaMappingInlinePass;
import vadl.rtl.passes.MiaMappingOptimizePass;
import vadl.rtl.passes.RtlConfigurationPass;
import vadl.rtl.passes.StageOrderingPass;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;

/**
 * RTL-specific pipeline extensions.
 */
public final class RtlPassOrders {
  private RtlPassOrders() {
  }

  public static PassOrder extendRtl(PassOrder order, RtlConfiguration config) {
    order.skip(NormalizeFieldsToFieldAccessFunctionsPass.class);
    order.skip(vadl.gcb.passes.RenamingConflictingRegistersPass.class);
    order.skip(OverwriteInputOperandsPass.class);

    order.add(new RtlConfigurationPass(config));
    order.add(new StageOrderingPass(config));
    order.add(new InstructionProgressGraphCreationPass(config))
        .add(new MiaMappingCreationPass(config))
        .add(new InstructionProgressGraphMergePass(config))
        .add(new MiaMappingOptimizePass(config))
        .add(new InstructionProgressGraphLowerPass(config))
        .add(new InstructionProgressGraphNamePass(config));

    PassOrderPipelineUtils.addHtmlDump(order, config,
        "mia-map",
        "MiA after mapping instruction behavior");

    order.add(new HazardAnalysisPass(config));

    if (config.isEmitDebugPrint()) {
      order.add(new DebugOutputPass(config));
    }

    order.add(new MiaMappingInlinePass(config));

    if (config.isEmitRVFI()) {
      order.add(new EmitRVFIOutputsPass(config));
    }

    order.add(new ForwardingLogicPass(config))
        .add(new ControlLogicPass(config));

    PassOrderPipelineUtils.addHtmlDump(order, config,
        "mia-inline",
        "MiA after inlining instruction behavior");

    if (config.getDecoderOptions().getGenerator() != RTL_TABLE) {
      VdtPassOrders.addDecodePasses(order, config);
    }

    if (!config.isDryRun()) {
      addRtlEmitPasses(order, config);
    }

    return order;
  }

  private static void addRtlEmitPasses(PassOrder order, RtlConfiguration config) {
    order.add(new EmitBuildSbtPass(config))
        .add(new EmitModulesPass(config))
        .add(new EmitVadlLibPass(config))
        .add(new EmitCoreTestPass(config))
        .add(new EmitCoreEmitPass(config))
        .add(new EmitElfSimPass(config))
        .add(new EmitSimMemSvPass(config))
        .add(new EmitSimMemCppPass(config))
        .add(new EmitScalafmtConfigPass(config))
        .add(new EmitRtlMakefilePass(config))
        .add(new EmitRtlDevcontainerConfigPass(config))
        .add(new EmitRtlDevcontainerDockerComposePass(config))
        .add(new CleanupEmitDirectoryPass(config));
  }
}
