// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import static vadl.iss.template.IssDefaultRenderingPass.issDefault;

import java.io.IOException;
import java.util.Optional;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.dump.DumpIssInstructionGraphsPass;
import vadl.iss.passes.IssBitfieldWriteLoweringPass;
import vadl.iss.passes.IssBuiltInArgTruncOptPass;
import vadl.iss.passes.IssCFunctionExtractionPass;
import vadl.iss.passes.IssConfigurationPass;
import vadl.iss.passes.IssExecStrategyPass;
import vadl.iss.passes.IssExtractOptimizationPass;
import vadl.iss.passes.IssGdbInfoExtractionPass;
import vadl.iss.passes.IssHardcodedTcgAddOnPass;
import vadl.iss.passes.IssInfoRetrievalPass;
import vadl.iss.passes.IssLoopUnrollPass;
import vadl.iss.passes.IssMemoryAccessTransformationPass;
import vadl.iss.passes.IssMemoryDetectionPass;
import vadl.iss.passes.IssNormalizationPass;
import vadl.iss.passes.IssPcAccessConversionPass;
import vadl.iss.passes.IssRegisterAccessInfoRetrievalPass;
import vadl.iss.passes.IssRegisterAccessLoweringPass;
import vadl.iss.passes.IssSelectLoweringPass;
import vadl.iss.passes.IssTcgSchedulingPass;
import vadl.iss.passes.IssTcgVAllocationPass;
import vadl.iss.passes.opDecomposition.IssOpDecompositionPass;
import vadl.iss.passes.safeResourceRead.IssSafeResourceReadPass;
import vadl.iss.passes.tcgLowering.IssTcgContextPass;
import vadl.iss.passes.tcgLowering.TcgBranchLoweringPass;
import vadl.iss.passes.tcgLowering.TcgOpLoweringPass;
import vadl.iss.template.gdb_xml.EmitIssGdbXmlPass;
import vadl.iss.template.hw.EmitIssHwMachineCPass;
import vadl.iss.template.target.EmitIssCpuHeaderPass;
import vadl.iss.template.target.EmitIssCpuParamHeaderPass;
import vadl.iss.template.target.EmitIssCpuQomHeaderPass;
import vadl.iss.template.target.EmitIssCpuSourcePass;
import vadl.iss.template.target.EmitIssDecodeTreePass;
import vadl.iss.template.target.EmitIssDoExcCIncPass;
import vadl.iss.template.target.EmitIssGdbStubPass;
import vadl.iss.template.target.EmitIssHelperCPass;
import vadl.iss.template.target.EmitIssHelperHPass;
import vadl.iss.template.target.EmitIssInsnTransCIncPass;
import vadl.iss.template.target.EmitIssMachinePass;
import vadl.iss.template.target.EmitIssTranslateCPass;
import vadl.lcb.passes.OverwriteInputOperandsPass;
import vadl.pass.PassOrder;
import vadl.viam.passes.DuplicateWriteDetectionPass;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.functionInliner.ArtificialResInlinerPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;

/**
 * ISS-specific pipeline extensions.
 */
public final class IssPassOrders {
  private IssPassOrders() {
  }

  public static PassOrder iss(IssConfiguration config) throws IOException {
    return extendIss(ViamPassOrders.viam(config), config);
  }

  public static PassOrder extendIss(PassOrder order, IssConfiguration config) {
    order.skip(ArtificialResInlinerPass.class);
    order.skip(FieldAccessInlinerPass.class);
    order.skip(NormalizeFieldsToFieldAccessFunctionsPass.class);
    order.skip(vadl.gcb.passes.RenamingConflictingRegistersPass.class);
    order.skip(OverwriteInputOperandsPass.class);
    // TODO: Fix duplicate-write detection for ISS helper-only register accesses
    //       and re-enable this shared VIAM validation pass for the ISS pipeline.
    order.skip(DuplicateWriteDetectionPass.class);

    order
        .add(new IssInfoRetrievalPass(config))
        .add(new IssConfigurationPass(config))
        .add(new IssMemoryDetectionPass(config))
        .add(new IssRegisterAccessLoweringPass(config))
        .add(new IssExecStrategyPass(config))
        .add(new IssBitfieldWriteLoweringPass(config))
        .add(new CanonicalizationPass(config))
        .add(new IssOpDecompositionPass(config))
        .add(new IssNormalizationPass(config))
        .add(new IssExtractOptimizationPass(config))
        .add(new IssMemoryAccessTransformationPass(config))
        .add(new IssBuiltInArgTruncOptPass(config))
        .add(new IssLoopUnrollPass(config))
        .add(new SideEffectSchedulingPass(config))
        .add(new IssSafeResourceReadPass(config))
        .add(new IssPcAccessConversionPass(config))
        .add(new IssTcgContextPass(config))
        .add(new IssSelectLoweringPass(config))
        .add(new IssTcgSchedulingPass(config))
        .add(new TcgBranchLoweringPass(config))
        .add(new TcgOpLoweringPass(config))
        .add(new IssHardcodedTcgAddOnPass(config))
        .add(new IssTcgVAllocationPass(config))
        .add(new IssGdbInfoExtractionPass(config))
        .add(new IssCFunctionExtractionPass(config))
        .add(new IssRegisterAccessInfoRetrievalPass(config));

    VdtPassOrders.addDecodePasses(order, config);

    if (config.dumpMode() == DumpMode.ISS_PASS_GRAPHS) {
      addIssInstructionGraphDumpPasses(order, config);
    }

    PassOrderPipelineUtils.addHtmlDump(order, config, "ISS Lowering Dump",
        "This dump is executed after the iss transformation passes were executed.",
        IssInfoRetrievalPass.class,
        IssConfiguration.class);

    if (!config.isDryRun()) {
      addIssEmitPasses(order, config);
    }

    return order;
  }

  private static PassOrder addIssInstructionGraphDumpPasses(PassOrder order,
                                                            GeneralConfiguration config) {
    return order.addBetweenEach((current, next) -> {
      if (current instanceof DumpIssInstructionGraphsPass) {
        return Optional.empty();
      }
      return Optional.of(new DumpIssInstructionGraphsPass(config));
    });
  }

  private static void addIssEmitPasses(PassOrder order, IssConfiguration config) {
    order
        .add(issDefault("meson.build", config))
        .add(issDefault("/include/vadl-builtins.h", config))
        .add(issDefault("/include/vadl-iss-builtins.h", config))
        .add(issDefault("/configs/devices/gen-arch-softmmu/default.mak", config))
        .add(issDefault("/configs/targets/gen-arch-softmmu.mak", config))
        .add(issDefault("/target/gen-arch/cpu.h", config))
        .add(issDefault("/include/disas/dis-asm.h", config))
        .add(issDefault("/include/sysemu/arch_init.h", config))
        .add(new EmitIssGdbXmlPass(config))
        .add(new EmitIssGdbStubPass(config))
        .add(issDefault("/tests/tcg/plugins/endstate.c", config))
        .add(issDefault("/tests/tcg/plugins/meson.build", config))
        .add(issDefault("/hw/Kconfig", config))
        .add(issDefault("/hw/meson.build", config))
        .add(issDefault("/hw/gen-arch/Kconfig", config))
        .add(issDefault("/hw/gen-arch/meson.build", config))
        .add(new EmitIssHwMachineCPass(config))
        .add(issDefault("/hw/gen-arch/gen-machine.h", config))
        .add(issDefault("/hw/gen-arch/boot.c", config))
        .add(issDefault("/hw/gen-arch/boot.h", config))
        .add(issDefault("/target/Kconfig", config))
        .add(issDefault("/target/meson.build", config))
        .add(issDefault("/target/gen-arch/trace-events", config))
        .add(issDefault("/target/gen-arch/trace.h", config))
        .add(issDefault("/target/gen-arch/Kconfig", config))
        .add(issDefault("/target/gen-arch/meson.build", config))
        .add(issDefault("/target/gen-arch/cpu-bits.h", config))
        .add(new EmitIssHelperCPass(config))
        .add(new EmitIssHelperHPass(config))
        .add(new EmitIssDoExcCIncPass(config))
        .add(new EmitIssCpuQomHeaderPass(config))
        .add(new EmitIssCpuParamHeaderPass(config))
        .add(new EmitIssCpuHeaderPass(config))
        .add(new EmitIssCpuSourcePass(config))
        .add(new EmitIssDecodeTreePass(config))
        .add(new EmitIssInsnTransCIncPass(config))
        .add(new EmitIssTranslateCPass(config))
        .add(new EmitIssMachinePass(config))
        .add(issDefault("/contrib/plugins/meson.build", config))
        .add(issDefault("/contrib/plugins/cosimulation.c", config));

    addUserModeEmitPasses(order, config);
  }

  private static void addUserModeEmitPasses(PassOrder order, IssConfiguration config) {
    var inputPath = config.inputPath();
    if (inputPath != null && inputPath.getFileName().endsWith("rv64ume.vadl")) {
      order
          .add(issDefault("/configs/targets/gen-arch-linux-user.mak", config))
          .add(issDefault("/linux-user/meson.build", config))
          .add(issDefault("/linux-user/elfload.c", config))
          .add(issDefault("/linux-user/syscall_defs.h", config))
          .add(issDefault("/linux-user/gen-arch/meson.build", config))
          .add(issDefault("/linux-user/gen-arch/cpu_loop.c", config))
          .add(issDefault("/linux-user/gen-arch/signal.c", config))
          .add(issDefault("/linux-user/gen-arch/sockbits.h", config))
          .add(issDefault("/linux-user/gen-arch/syscall.tbl", config))
          .add(issDefault("/linux-user/gen-arch/syscallhdr.sh", true, config))
          .add(issDefault("/linux-user/gen-arch/target_cpu.h", config))
          .add(issDefault("/linux-user/gen-arch/target_elf.h", config))
          .add(issDefault("/linux-user/gen-arch/target_errno_defs.h", config))
          .add(issDefault("/linux-user/gen-arch/target_fcntl.h", config))
          .add(issDefault("/linux-user/gen-arch/target_mman.h", config))
          .add(issDefault("/linux-user/gen-arch/target_prctl.h", config))
          .add(issDefault("/linux-user/gen-arch/target_proc.h", config))
          .add(issDefault("/linux-user/gen-arch/target_resource.h", config))
          .add(issDefault("/linux-user/gen-arch/target_signal.h", config))
          .add(issDefault("/linux-user/gen-arch/target_structs.h", config))
          .add(issDefault("/linux-user/gen-arch/target_syscall.h", config))
          .add(issDefault("/linux-user/gen-arch/termbits.h", config));
    }
  }
}
