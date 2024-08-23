package vadl.pass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPassForDecoding;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.dummyAbi.DummyAbiPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

/**
 * This class defines the order in which the {@link PassManager} should run them.
 */
public final class PassOrder {

  public static List<Pass> viam(GeneralConfiguration configuration) {
    List<Pass> passes = new ArrayList<>();

    passes.add(new TypeCastEliminationPass(configuration));
    passes.add(new FunctionInlinerPass(configuration));
    passes.add(new AlgebraicSimplificationPass(configuration));

    return passes;
  }

  public static List<Pass> gcbAndCppCodeGen(GcbConfiguration gcbConfiguration) {
    List<Pass> passes = new ArrayList<>(viam(gcbConfiguration));

    passes.add(new GenerateFieldAccessEncodingFunctionPass(gcbConfiguration));
    passes.add(new FieldNodeReplacementPassForDecoding(gcbConfiguration));
    passes.add(new CppTypeNormalizationForEncodingsPass(gcbConfiguration));
    passes.add(new CppTypeNormalizationForDecodingsPass(gcbConfiguration));
    passes.add(new CppTypeNormalizationForPredicatesPass(gcbConfiguration));

    return passes;
  }

  /**
   * This is the pass order which must be executed to get a LLVM compiler.
   */
  public static List<Pass> lcb(LcbConfiguration configuration, ProcessorName processorName)
      throws IOException {
    List<Pass> passes = new ArrayList<>(gcbAndCppCodeGen(configuration));
    passes.add(new IsaMatchingPass(configuration));
    passes.add(new LlvmLoweringPass(configuration));
    passes.add(new vadl.lcb.clang.lib.Driver.ToolChains.EmitClangToolChainFilePass(configuration,
        processorName));
    passes.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetHeaderFilePass(configuration,
        processorName));
    passes.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetsFilePass(configuration,
        processorName));
    passes.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetCppFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.clang.lib.Basic.EmitClangBasicCMakeFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenModuleCMakeFilePass(configuration,
        processorName));
    passes.add(new vadl.lcb.template.clang.lib.CodeGen.Targets.EmitClangCodeGenTargetFilePass(
        configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenTargetInfoHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.clang.lib.CodeGen.EmitCodeGenModuleFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.lld.ELF.EmitLldDriverFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.lld.ELF.EmitLldELFCMakeFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lld.ELF.EmitLldTargetHeaderFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lld.ELF.Arch.EmitLldManualEncodingHeaderFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitImmediateUtilsHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldArchFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lld.ELF.EmitLldTargetCppFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.EmitLcbMakeFilePass(configuration, processorName));
    passes.add(new vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass(
        configuration, processorName));
    passes.add(
        new vadl.lcb.include.llvm.BinaryFormat.EmitElfHeaderFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.include.llvm.Object.EmitELFObjectHeaderFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Misc.EmitBenchmarkRegisterHeaderFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.EmitFrameLoweringCppFilePass(configuration,
        processorName));
    passes.add(new vadl.lcb.template.lib.Target.EmitMachineFunctionInfoHeaderFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitTargetObjectFileCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitInstrInfoHeaderFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitExpandPseudoHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitDAGToDAGIselHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserHeaderFilePass(
            configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCppFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCMakeFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserCppFilePass(
            configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitDAGToDAGISelCppFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.lib.Target.EmitAsmPrinterHeaderFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitCallingConvTableGenFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitRegisterInfoHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.Utils.EmitBaseInfoFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitTargetTableGenFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitTargetHeaderFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitAsmPrinterCppFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitSubTargetHeaderFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitFrameLoweringHeaderFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.EmitPassConfigHeaderFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCMakeFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitISelLoweringCppFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCMakeFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCppFile(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitPassConfigCppFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitSubTargetCppFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitTargetCMakeFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterCppFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprHeaderFilePass(configuration,
        processorName));
    passes.add(
        new EmitMCInstLowerCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCMakeFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstrPrinterHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendCppFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoCppFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerHeaderFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitTargetStreamerHeaderFilePass(
        configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoHeaderFilePass(configuration,
            processorName));
    passes.add(
        new EmitMCInstLowerHeaderFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterHeaderFilePass(
        configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitFixupKindsHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstrPrinterCppFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsHeaderFilePass(configuration,
            processorName));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderHeaderFilePass(
        configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitInstrInfoCppFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetMachineCppFilePass(configuration,
        processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitTargetMachineHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitExpandPseudoCppFilePass(configuration, processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitTargetObjectFileHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitISelLoweringHeaderFilePass(configuration,
            processorName));
    passes.add(
        new vadl.lcb.template.lib.TargetParser.EmitTripleCppFilePass(configuration, processorName));
    passes.add(new vadl.lcb.template.lib.Object.EmitElfCppFilePass(configuration, processorName));

    return passes;
  }
}
