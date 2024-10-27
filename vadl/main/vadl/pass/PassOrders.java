package vadl.pass;

import static vadl.iss.template.IssDefaultRenderingPass.issDefault;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPassForDecoding;
import vadl.dump.HtmlDumpPass;
import vadl.gcb.passes.assembly.AssemblyConcatBuiltinMergingPass;
import vadl.gcb.passes.assembly.AssemblyReplacementNodePass;
import vadl.gcb.passes.encodingGeneration.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.pseudo.PseudoInstructionArgumentReplacementPass;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForPredicatesPass;
import vadl.iss.passes.IssConfigurationPass;
import vadl.iss.passes.IssVerificationPass;
import vadl.iss.passes.tcgLowering.TcgLoweringPass;
import vadl.iss.template.target.EmitIssCpuHeaderPass;
import vadl.iss.template.target.EmitIssCpuParamHeaderPass;
import vadl.iss.template.target.EmitIssCpuQomHeaderPass;
import vadl.iss.template.target.EmitIssCpuSourcePass;
import vadl.iss.template.target.EmitIssInsnDecodePass;
import vadl.iss.template.target.EmitIssMachinePass;
import vadl.iss.template.target.EmitIssTranslatePass;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.ConstMatPseudoInstructionArgumentReplacementPass;
import vadl.lcb.passes.llvmLowering.ConstMaterialisationPseudoExpansionFunctionGeneratorPass;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationTableGenRecordPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterHeaderFilePass;
import vadl.viam.passes.InstructionResourceAccessAnalysisPass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.dummyAbi.DummyAbiPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
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

    order.add(new ViamVerificationPass(configuration));

    order.add(new TypeCastEliminationPass(configuration));
    order.add(new DummyAbiPass(configuration));
    // TODO: @kper do you see any fix for this?
    // Note: we run the counter-access resolving pass before the func inliner pass
    // because the lcb uses the uninlined version of the instructions.
    // However, this might miss a lot of opportunities to statically resolve counter-accesses
    // as the canonicalization runs at a later point.
    order.add(new StaticCounterAccessResolvingPass(configuration));
    order.add(new FunctionInlinerPass(configuration));
    order.add(new SideEffectConditionResolvingPass(configuration));

    // Common optimizations
    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));

    order.add(new InstructionResourceAccessAnalysisPass(configuration));

    // verification after viam optimizations
    order.add(new ViamVerificationPass(configuration));

    if (configuration.doDump()) {
      var config = HtmlDumpPass.Config.from(
          configuration,
          "viamOptimizations",
          "All common VIAM optimization that are required by most generators are executed."
      );
      order.add(new HtmlDumpPass(config));
    }

    return order;
  }

  /**
   * Return the gcb and cppcodegen passes.
   */
  public static PassOrder gcbAndCppCodeGen(GcbConfiguration gcbConfiguration) throws IOException {
    var order = viam(gcbConfiguration);

    order.add(new IdentifyFieldUsagePass(gcbConfiguration));
    //order.add(new AddMissingFieldAccessesPass(gcbConfiguration));
    order.add(new GenerateFieldAccessEncodingFunctionPass(gcbConfiguration));
    order.add(new FieldNodeReplacementPassForDecoding(gcbConfiguration));
    order.add(new CppTypeNormalizationForEncodingsPass(gcbConfiguration));
    order.add(new CppTypeNormalizationForDecodingsPass(gcbConfiguration));
    order.add(new CppTypeNormalizationForPredicatesPass(gcbConfiguration));
    order.add(new AssemblyReplacementNodePass(gcbConfiguration));
    order.add(new AssemblyConcatBuiltinMergingPass(gcbConfiguration));
    order.add(new PseudoInstructionArgumentReplacementPass(gcbConfiguration));
    order.add(new PseudoExpansionFunctionGeneratorPass(gcbConfiguration));

    if (gcbConfiguration.doDump()) {
      var config = HtmlDumpPass.Config.from(gcbConfiguration,
          "gcbProcessing",
          "Now the gcb produced all necessary encoding function for field accesses "
              + "and normalized VIAM types to Cpp types."
      );
      order.add(new HtmlDumpPass(config));
    }

    return order;
  }

  /**
   * This is the pass order which must be executed to get a LLVM compiler.
   */
  public static PassOrder lcb(LcbConfiguration configuration)
      throws IOException {
    var order = gcbAndCppCodeGen(configuration);
    order.add(new IsaMatchingPass(configuration));
    order.add(new GenerateRegisterClassesPass(configuration));
    order.add(new LlvmLoweringPass(configuration));
    order.add(new GenerateTableGenMachineInstructionRecordPass(configuration));
    order.add(new GenerateTableGenPseudoInstructionRecordPass(configuration));
    order.add(new GenerateTableGenImmediateRecordPass(configuration));
    order.add(new GenerateConstantMaterialisationPass(configuration));
    order.add(new GenerateConstantMaterialisationTableGenRecordPass(configuration));
    order.add(new ConstMatPseudoInstructionArgumentReplacementPass(configuration));
    order.add(new ConstMaterialisationPseudoExpansionFunctionGeneratorPass(configuration));
    order.add(new GenerateLinkerComponentsPass(configuration));

    if (configuration.doDump()) {
      var config = HtmlDumpPass.Config.from(
          configuration,
          "lcbLlvmLowering",
          "The LCB did ISA matching to and lowered common VIAM nodes to LLVM specific"
              + "nodes."
      );
      order.add(new HtmlDumpPass(config));
    }

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
    // iss function passes
    order
        .add(new IssVerificationPass(config))
        .add(new IssConfigurationPass(config))
        .add(new TcgLoweringPass(config))

        .add(new ViamVerificationPass(config))
    ;


    if (config.doDump()) {
      order.add(new HtmlDumpPass(HtmlDumpPass.Config.from(config, "ISS Lowering Dump", """
          This dump is executed after the iss transformation passes were executed.
          """)));
    }

    // add iss template emitting passes to order
    addIssEmitPasses(order, config);

    if (config.doDump()) {
      order.add(new HtmlDumpPass(HtmlDumpPass.Config.from(config, "ISS Rendering Dump", """
          This dump is executed after the iss got rendered passes were executed.
          """)));
    }


    return order;
  }

  private static void addIssEmitPasses(PassOrder order, IssConfiguration config) {
    order
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
        .add(issDefault("/hw/gen-arch/virt.c", config))
        .add(issDefault("/hw/gen-arch/virt.h", config))
        .add(issDefault("/hw/gen-arch/boot.c", config))
        .add(issDefault("/hw/gen-arch/boot.h", config))

        // target rendering
        .add(issDefault("/target/Kconfig", config))
        .add(issDefault("/target/meson.build", config))
        .add(issDefault("/target/gen-arch/Kconfig", config))
        .add(issDefault("/target/gen-arch/meson.build", config))
        .add(issDefault("/target/gen-arch/helper.c", config))
        .add(issDefault("/target/gen-arch/helper.h", config))
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
        // target/gen-arch/translate.c
        .add(new EmitIssTranslatePass(config))
        // target/gen-arch/machine.c
        .add(new EmitIssMachinePass(config))

    ;
  }

}
