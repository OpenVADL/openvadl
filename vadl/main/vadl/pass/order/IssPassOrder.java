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

import static vadl.iss.template.IssDefaultRenderingPass.issDefault;

import java.io.IOException;
import java.util.Optional;
import vadl.configuration.DumpMode;
import vadl.configuration.IssConfiguration;
import vadl.dump.DumpIssInstructionGraphsPass;
import vadl.gcb.passes.RenamingConflictingRegistersPass;
import vadl.iss.passes.UmeHardcodedRiscvDefinitionPass;
import vadl.iss.passes.UmeTemplateRenderingPass;
import vadl.iss.passes.common.IssApplyMemoryEndiannessPass;
import vadl.iss.passes.common.IssBitfieldWriteLoweringPass;
import vadl.iss.passes.common.IssBuiltInArgTruncOptPass;
import vadl.iss.passes.common.IssCommonExprSavePass;
import vadl.iss.passes.common.IssConfigurationPass;
import vadl.iss.passes.common.IssExtractOptimizationPass;
import vadl.iss.passes.common.IssGdbInfoExtractionPass;
import vadl.iss.passes.common.IssInfoRetrievalPass;
import vadl.iss.passes.common.IssLoopUnrollPass;
import vadl.iss.passes.common.IssMemoryAccessTransformationPass;
import vadl.iss.passes.common.IssMemoryDetectionPass;
import vadl.iss.passes.common.IssNormalizationPass;
import vadl.iss.passes.common.IssRegisterAccessInfoRetrievalPass;
import vadl.iss.passes.common.IssRegisterAccessLoweringPass;
import vadl.iss.passes.common.IssScheduleIndirectJumpsPass;
import vadl.iss.passes.common.IssTensorAssignmentToForallPass;
import vadl.iss.passes.common.opDecomposition.IssOpDecompositionPass;
import vadl.iss.passes.common.planning.IssExecStrategyPass;
import vadl.iss.passes.common.safeResourceRead.IssSafeResourceReadPass;
import vadl.iss.passes.helper.IssCFunctionExtractionPass;
import vadl.iss.passes.tcg.IssSelectLoweringPass;
import vadl.iss.passes.tcg.IssTcgSchedulingPass;
import vadl.iss.passes.tcg.IssTcgVAllocationPass;
import vadl.iss.passes.tcg.UmeSyntheticSyscallExceptionPass;
import vadl.iss.passes.tcg.lowering.IssTcgContextPass;
import vadl.iss.passes.tcg.lowering.TcgBranchLoweringPass;
import vadl.iss.passes.tcg.lowering.TcgOpLoweringPass;
import vadl.iss.passes.vector.IssDirectGvecLoweringPass;
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
import vadl.viam.passes.ArtificialResPartialAccessExpansionPass;
import vadl.viam.passes.NormalizeFieldsToFieldAccessFunctionsPass;
import vadl.viam.passes.RegisterTensorPartialAccessExpansionPass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.functionInliner.ArtificialResInlinerPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;

/**
 * Builds the pass order used for ISS generation.
 */
public final class IssPassOrder {
  private IssPassOrder() {
  }

  /**
   * Creates the pass order.
   */
  public static PassOrder create(IssConfiguration config) throws IOException {
    var order = ViamPassOrder.create(config);

    order.add(new UmeHardcodedRiscvDefinitionPass(config));

    order.skip(ArtificialResInlinerPass.class);
    order.skip(FieldAccessInlinerPass.class);
    order.skip(NormalizeFieldsToFieldAccessFunctionsPass.class);
    order.skip(RenamingConflictingRegistersPass.class);
    order.skip(OverwriteInputOperandsPass.class);

    // Skip partial (alias) register extension passes as the ISS is able to handle them downstream
    // in an optimized way.
    order.skip(RegisterTensorPartialAccessExpansionPass.class);
    order.skip(ArtificialResPartialAccessExpansionPass.class);

    addCommonPasses(order, config);
    addScalarTcgPasses(order, config);
    addHelperPasses(order, config);

    OrderSupport.addDecodePasses(order, config);
    if (config.dumpMode() == DumpMode.ISS_PASS_GRAPHS) {
      addIssInstructionGraphDumpPasses(order, config);
    }

    OrderSupport.addHtmlDump(order, config, "ISS Lowering Dump",
        "This dump is executed after the iss transformation passes were executed.",
        IssInfoRetrievalPass.class,
        IssConfiguration.class);

    if (!config.isDryRun()) {
      addIssTemplatePasses(order, config);
    }
    return order;
  }

  private static void addCommonPasses(PassOrder order, IssConfiguration config) {
    order.add(new IssInfoRetrievalPass(config))
        .add(new IssConfigurationPass(config))
        .add(new IssApplyMemoryEndiannessPass(config))
        .add(new IssMemoryDetectionPass(config))
        .add(new IssRegisterAccessLoweringPass(config))
        .add(new IssBitfieldWriteLoweringPass(config))
        .add(new IssTensorAssignmentToForallPass(config))
        .add(new CanonicalizationPass(config))
        .add(new IssExecStrategyPass(config))
        .add(new IssDirectGvecLoweringPass(config))
        .add(new IssOpDecompositionPass(config))
        .add(new IssNormalizationPass(config))
        .add(new IssExtractOptimizationPass(config))
        .add(new IssMemoryAccessTransformationPass(config))
        .add(new IssBuiltInArgTruncOptPass(config))
        .add(new IssLoopUnrollPass(config))
        .add(new SideEffectSchedulingPass(config))
        .add(new IssSafeResourceReadPass(config))
        .add(new IssCommonExprSavePass(config))
        .add(new IssScheduleIndirectJumpsPass(config));
  }

  private static void addScalarTcgPasses(PassOrder order, IssConfiguration config) {
    order.add(new IssTcgContextPass(config))
        .add(new IssSelectLoweringPass(config))
        .add(new IssTcgSchedulingPass(config))
        .add(new TcgBranchLoweringPass(config))
        .add(new TcgOpLoweringPass(config))
        .add(new UmeSyntheticSyscallExceptionPass(config))
        .add(new IssTcgVAllocationPass(config));
  }

  private static void addHelperPasses(PassOrder order, IssConfiguration config) {
    order.add(new IssGdbInfoExtractionPass(config))
        .add(new IssCFunctionExtractionPass(config))
        .add(new IssRegisterAccessInfoRetrievalPass(config));
  }

  private static PassOrder addIssInstructionGraphDumpPasses(PassOrder order,
                                                            IssConfiguration config) {
    return order.addBetweenEach((current, next) -> {
      if (current instanceof DumpIssInstructionGraphsPass) {
        return Optional.empty();
      }
      return Optional.of(new DumpIssInstructionGraphsPass(config));
    });
  }

  private static void addIssTemplatePasses(PassOrder order, IssConfiguration config) {
    order.add(issDefault("meson.build", config))
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
    // right now, we only emit those passes if we generate the ISS for RV64UME
    var inputPath = config.inputPath();
    if (inputPath != null && inputPath.getFileName().endsWith("rv64ume.vadl")) {
      order
          .add(issDefault("/configs/targets/gen-arch-linux-user.mak", config))

          .add(issDefault("/linux-user/meson.build", config))
          .add(issDefault("/linux-user/elfload.c", config))
          .add(issDefault("/linux-user/syscall_defs.h", config))

          .add(issDefault("/linux-user/gen-arch/meson.build", config))
          .add(new UmeTemplateRenderingPass(config, "cpu_loop.c"))
          .add(new UmeTemplateRenderingPass(config, "signal.c"))
          .add(issDefault("/linux-user/gen-arch/sockbits.h", config))
          .add(issDefault("/linux-user/gen-arch/syscall.tbl", config))
          .add(issDefault("/linux-user/gen-arch/syscallhdr.sh", true, config))
          .add(new UmeTemplateRenderingPass(config, "target_cpu.h"))

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
          .add(issDefault("/linux-user/gen-arch/termbits.h", config))
      ;
    }
  }

}
