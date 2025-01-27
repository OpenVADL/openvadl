package vadl.pass;

import static vadl.iss.template.IssDefaultRenderingPass.issDefault;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPassForDecoding;
import vadl.dump.CollectBehaviorDotGraphPass;
import vadl.dump.HtmlDumpPass;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.assembly.AssemblyConcatBuiltinMergingPass;
import vadl.gcb.passes.assembly.AssemblyReplacementNodePass;
import vadl.gcb.passes.encodingGeneration.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.pseudo.PseudoInstructionArgumentReplacementPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForPredicatesPass;
import vadl.iss.passes.IssConfigurationPass;
import vadl.iss.passes.IssHardcodedTcgAddOnPass;
import vadl.iss.passes.IssPcAccessConversionPass;
import vadl.iss.passes.IssTcgConstantSchedulingPass;
import vadl.iss.passes.IssTcgSchedulingPass;
import vadl.iss.passes.IssTcgVAllocationPass;
import vadl.iss.passes.IssVarSsaAssignment;
import vadl.iss.passes.IssVerificationPass;
import vadl.iss.passes.decode.qemu.QemuDecodeLoweringPass;
import vadl.iss.passes.decode.qemu.QemuDecodeSymbolResolvingPass;
import vadl.iss.passes.opDecomposition.IssOpDecompositionPass;
import vadl.iss.passes.safeResourceRead.IssSafeResourceReadPass;
import vadl.iss.passes.tcgLowering.TcgBranchLoweringPass;
import vadl.iss.passes.tcgLowering.TcgOpLoweringPass;
import vadl.iss.template.hw.EmitIssVirtCPass;
import vadl.iss.template.target.EmitIssCpuHeaderPass;
import vadl.iss.template.target.EmitIssCpuParamHeaderPass;
import vadl.iss.template.target.EmitIssCpuQomHeaderPass;
import vadl.iss.template.target.EmitIssCpuSourcePass;
import vadl.iss.template.target.EmitIssMachinePass;
import vadl.iss.template.target.EmitIssTranslatePass;
import vadl.iss.template.target.decode.qemu.EmitIssInsnAccessFunctionPass;
import vadl.iss.template.target.decode.qemu.EmitIssInsnAccessHeaderPass;
import vadl.iss.template.target.decode.qemu.EmitIssInsnDecodePass;
import vadl.iss.template.target.decode.vdt.EmitIssDecodeTreePass;
import vadl.lcb.passes.DummyAnnotationPass;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.compensation.CompensationPatternPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitConstMatIntCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitConstMatIntHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterHeaderFilePass;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.viam.Specification;
import vadl.viam.passes.DuplicateWriteDetectionPass;
import vadl.viam.passes.InstructionResourceAccessAnalysisPass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.behaviorRewrite.BehaviorRewritePass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.dummyPasses.DummyAbiPass;
import vadl.viam.passes.dummyPasses.DummyMipPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass;
import vadl.viam.passes.staticCounterAccess.StaticCounterAccessResolvingPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * This class contains static methods that define the individual pass orders for different
 * generation targets (e.g., LCB, ISS, ...).
 */
public class PassOrders {

  /**
   * Return the viam passes.
   */
  public static PassOrder viam(GeneralConfiguration configuration) throws IOException {
    var order = new PassOrder();

    // this is just a pseudo pass to add the behavior to the HTML dump
    // at the stage directly after the VIAM creation.
    order.add(new ViamCreationPass(configuration));

    addHtmlDump(order, configuration, "VIAM Creation",
        "Dump directly after frontend generated VIAM.");

    order.add(new ViamVerificationPass(configuration));

    order.add(new TypeCastEliminationPass(configuration));

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
    order.add(new SideEffectConditionResolvingPass(configuration));
    // requires SideEffectConditionResolvingPass to work
    order.add(new DuplicateWriteDetectionPass(configuration));

    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));
    order.add(new InstructionResourceAccessAnalysisPass(configuration));

    // verification after viam optimizations
    order.add(new ViamVerificationPass(configuration));

    addHtmlDump(order, configuration, "viamOptimizations",
        "All common VIAM optimization that are required by most generators are executed.",
        InstructionResourceAccessAnalysisPass.class
    );

    return order;
  }

  /**
   * Return the gcb and cppcodegen passes.
   */
  public static PassOrder gcbAndCppCodeGen(GcbConfiguration gcbConfiguration) throws IOException {
    var order = viam(gcbConfiguration);

    order.add(new DummyAbiPass(gcbConfiguration));
    // skip inlining of field access
    order.skip(FieldAccessInlinerPass.class);
    order.add(new IdentifyFieldUsagePass(gcbConfiguration));
    order.add(new GenerateFieldAccessEncodingFunctionPass(gcbConfiguration));
    order.add(new FieldNodeReplacementPassForDecoding(gcbConfiguration));
    order.add(new CppTypeNormalizationForEncodingsPass(gcbConfiguration));
    order.add(new CppTypeNormalizationForDecodingsPass(gcbConfiguration));
    order.add(new CppTypeNormalizationForPredicatesPass(gcbConfiguration));
    order.add(new AssemblyReplacementNodePass(gcbConfiguration));
    order.add(new AssemblyConcatBuiltinMergingPass(gcbConfiguration));
    order.add(new PseudoInstructionArgumentReplacementPass(gcbConfiguration));
    order.add(new PseudoExpansionFunctionGeneratorPass(gcbConfiguration));

    addHtmlDump(order, gcbConfiguration, "gcbProcessing",
        "Now the gcb produced all necessary encoding function for field accesses "
            + "and normalized VIAM types to Cpp types.");

    return order;
  }

  /**
   * This is the pass order which must be executed to get a LLVM compiler.
   */
  public static PassOrder lcb(LcbConfiguration configuration)
      throws IOException {
    var order = gcbAndCppCodeGen(configuration);
    // skip inlining of field access
    order.skip(FieldAccessInlinerPass.class);
    order.add(new DummyAnnotationPass(configuration));

    order.add(new IsaMachineInstructionMatchingPass(configuration));
    order.add(new IsaPseudoInstructionMatchingPass(configuration));
    order.add(new GenerateRegisterClassesPass(configuration));
    order.add(new LlvmLoweringPass(configuration));
    order.add(new GenerateTableGenMachineInstructionRecordPass(configuration));
    order.add(new GenerateTableGenPseudoInstructionRecordPass(configuration));
    order.add(new GenerateTableGenImmediateRecordPass(configuration));
    order.add(new CompensationPatternPass(configuration));
    order.add(new GenerateLinkerComponentsPass(configuration));

    addHtmlDump(order, configuration,
        "lcbLlvmLowering",
        "The LCB did ISA matching to and lowered common VIAM nodes to LLVM specific"
            + "nodes.");

    order.add(new vadl.lcb.clang.lib.Driver.ToolChains.EmitClangToolChainFilePass(configuration));
    order.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetHeaderFilePass(configuration));
    order.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetsFilePass(configuration));
    order.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetCppFilePass(configuration));
    order.add(new vadl.lcb.template.clang.lib.Basic.EmitClangBasicCMakeFilePass(configuration));
    order.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenModuleCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.clang.lib.CodeGen.Targets.EmitClangCodeGenTargetFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenTargetInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.clang.lib.CodeGen.EmitCodeGenModuleFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldDriverFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldELFCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldTargetHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldManualEncodingHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.Arch.EmitImmediateUtilsHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.Arch.EmitLldArchFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldTargetCppFilePass(configuration));
    order.add(new vadl.lcb.template.EmitLcbMakeFilePass(configuration));
    order.add(new vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass(
        configuration));
    order.add(new vadl.lcb.include.llvm.BinaryFormat.EmitElfHeaderFilePass(configuration));
    order.add(new vadl.lcb.include.llvm.Object.EmitELFObjectHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Misc.EmitBenchmarkRegisterHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitFrameLoweringCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.EmitMachineFunctionInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetObjectFileCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitInstrInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitExpandPseudoHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitDAGToDAGIselHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserHeaderFilePass(
            configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserCppFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.EmitDAGToDAGISelCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitAsmPrinterHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitCallingConvTableGenFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.Utils.EmitBaseInfoFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetTableGenFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitAsmPrinterCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitSubTargetHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitFrameLoweringHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitPassConfigHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitISelLoweringCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCppFile(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitPassConfigCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitSubTargetCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMcTargetDescCMakeFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerCppFilePass(configuration));
    order.add(
        new EmitConstMatIntHeaderFilePass(configuration));
    order.add(
        new EmitConstMatIntCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterCppFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprHeaderFilePass(configuration));
    order.add(new EmitMCInstLowerCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCppFilePass(configuration));
    order.add(new EmitInstPrinterHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitTargetStreamerHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoHeaderFilePass(configuration));
    order.add(new EmitMCInstLowerHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterHeaderFilePass(
        configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitFixupKindsHeaderFilePass(configuration));
    order.add(
        new EmitInstPrinterCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderHeaderFilePass(
        configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitInstrInfoCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetMachineCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetMachineHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitExpandPseudoCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetObjectFileHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitISelLoweringHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.include.llvm.TargetParser.EmitTripleHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.TargetParser.EmitTripleCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Object.EmitElfCppFilePass(configuration));

    return order;
  }

  /**
   * Constructs the pass order used to generate the ISS (QEMU) from a VADL specification.
   */
  public static PassOrder iss(IssConfiguration config) throws IOException {
    var order = viam(config);

    // skip inlining of field access
    order.skip(FieldAccessInlinerPass.class);

    // TODO: Remove once frontend creates it
    order.add(new DummyAbiPass(config))
        .add(new DummyMipPass(config));

    // iss function passes
    order
        .add(new IssVerificationPass(config))
        .add(new IssConfigurationPass(config))
        .add(new IssOpDecompositionPass(config))
        .add(new SideEffectSchedulingPass(config))
        .add(new IssSafeResourceReadPass(config))
        .add(new IssPcAccessConversionPass(config))
        .add(new IssTcgSchedulingPass(config))
        .add(new IssVarSsaAssignment(config))
        .add(new TcgBranchLoweringPass(config))
        .add(new IssTcgConstantSchedulingPass(config))
        .add(new TcgOpLoweringPass(config))
        .add(new IssHardcodedTcgAddOnPass(config))
        .add(new IssTcgVAllocationPass(config))
    ;

    addDecodePasses(order, config);

    addHtmlDump(order, config, "ISS Lowering Dump",
        "This dump is executed after the iss transformation passes were executed.",
        IssVerificationPass.class,
        IssConfiguration.class);

    order.add(new ViamVerificationPass(config));

    // add iss template emitting passes to order
    addIssEmitPasses(order, config);

    return order;
  }

  private static void addIssEmitPasses(PassOrder order, IssConfiguration config) {
    order
        // top-level meson build. just because we want to add target trace-events
        .add(issDefault("meson.build", config))

        // config rendering
        .add(issDefault("/configs/devices/gen-arch-softmmu/default.mak", config))
        .add(issDefault("/configs/targets/gen-arch-softmmu.mak", config))
        .add(issDefault("/target/gen-arch/cpu.h", config))

        // arch init rendering
        .add(issDefault("/include/disas/dis-asm.h", config))
        .add(issDefault("/include/sysemu/arch_init.h", config))

        // hardware rendering
        .add(issDefault("/hw/Kconfig", config))
        .add(issDefault("/hw/meson.build", config))
        .add(issDefault("/hw/gen-arch/Kconfig", config))
        .add(issDefault("/hw/gen-arch/meson.build", config))
        .add(new EmitIssVirtCPass(config))
        .add(issDefault("/hw/gen-arch/virt.h", config))
        .add(issDefault("/hw/gen-arch/boot.c", config))
        .add(issDefault("/hw/gen-arch/boot.h", config))

        // target rendering
        .add(issDefault("/target/Kconfig", config))
        .add(issDefault("/target/meson.build", config))
        .add(issDefault("/target/gen-arch/trace-events", config))
        .add(issDefault("/target/gen-arch/trace.h", config))
        .add(issDefault("/target/gen-arch/Kconfig", config))
        .add(issDefault("/target/gen-arch/meson.build", config))
        .add(issDefault("/target/gen-arch/vadl-builtins.h", config))
        .add(issDefault("/target/gen-arch/helper.c", config))
        .add(issDefault("/target/gen-arch/helper.h", config))
        .add(issDefault("/target/gen-arch/cpu-bits.h", config))
        // target/gen-arch/cpu-qom.h
        .add(new EmitIssCpuQomHeaderPass(config))
        // target/gen-arch/cpu-param.h
        .add(new EmitIssCpuParamHeaderPass(config))
        // target/gen-arch/cpu.h
        .add(new EmitIssCpuHeaderPass(config))
        // target/gen-arch/cpu.c
        .add(new EmitIssCpuSourcePass(config))
        // target/gen-arch/insn.decode
        .add(new EmitIssInsnDecodePass(config))
        // target/gen-arch/insn-access.h
        .add(new EmitIssInsnAccessHeaderPass(config))
        // target/gen-arch/insn-access.c
        .add(new EmitIssInsnAccessFunctionPass(config))
        // target/gen-arch/vdt-decode.c
        .add(new EmitIssDecodeTreePass(config))
        // target/gen-arch/translate.c
        .add(new EmitIssTranslatePass(config))
        // target/gen-arch/machine.c
        .add(new EmitIssMachinePass(config))

    ;
  }

  // TODO: @mraschhofer write documentation
  private static void addDecodePasses(PassOrder order, IssConfiguration config) {

    // TODO: Add config params to switch between decoder implementations. For now we generate
    // code for both. The implementation used by the ISS is determined by the linking of the
    // generated resources.

    // QEMU Decode Passes
    order.add(new QemuDecodeLoweringPass(config));
    order.add(new QemuDecodeSymbolResolvingPass(config));

    // VDT Decode Passes
    order.add(new VdtLoweringPass(config));
  }

  /**
   * Adds all necessary passes for a html dump of the VIAM if the config whishes to dump.
   *
   * @param order       into which the passes will be inserted.
   * @param config      from which to decide if a dump is wanted.
   * @param phase       is the name of the dump.
   * @param description for the dump.
   * @param exclusions  for which no behavior graph should be collected.
   * @return the modified passorder.
   */
  private static PassOrder addHtmlDump(PassOrder order, GeneralConfiguration config,
                                       String phase, String description, Class<?>... exclusions) {

    if (config.doDump()) {
      addDumpBehaviorCollectionPasses(order, config, exclusions);
      var htmlConfig = HtmlDumpPass.Config.from(config, phase, description);
      order.add(new HtmlDumpPass(htmlConfig));
    }

    return order;
  }

  /**
   * Adds a {@link CollectBehaviorDotGraphPass} after each existing pass in the pass order.
   * This function is idempotent, so calling it on the same pass order will not add
   * more behavior collection passes.
   *
   * @param order      to add collection passes to
   * @param exceptions pass classes that don't change the behavior and therefore should not have
   *                   behavior collection pass successor.
   * @return the passed order
   */
  private static PassOrder addDumpBehaviorCollectionPasses(PassOrder order,
                                                           GeneralConfiguration config,
                                                           Class<?>... exceptions) {
    if (!config.doDump()) {
      return order;
    }

    var dontRun = Streams.concat(Stream.of(
        ViamVerificationPass.class,
        AbstractTemplateRenderingPass.class,
        CollectBehaviorDotGraphPass.class
    ), Stream.of(exceptions)).toList();

    order.addBetweenEach((prev, next) -> {

      if (dontRun.stream().anyMatch(a -> a.isInstance(prev))) {
        // if previous is any of the dont run pass classes, don't add the collection behavior
        return Optional.empty();
      }

      if (next.orElse(null) instanceof CollectBehaviorDotGraphPass) {
        // don't add a collection pass, if the next one is already a collection pass.
        return Optional.empty();
      }

      return Optional.of(new CollectBehaviorDotGraphPass(config));
    });
    return order;
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
