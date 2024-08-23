package vadl.pass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPassForDecoding;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
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

  /**
   * Return the viam passes.
   */
  public static List<Pass> viam(GeneralConfiguration configuration) {
    List<Pass> passes = new ArrayList<>();

    passes.add(new DummyAbiPass(configuration));
    passes.add(new TypeCastEliminationPass(configuration));
    passes.add(new FunctionInlinerPass(configuration));
    passes.add(new AlgebraicSimplificationPass(configuration));

    return passes;
  }

  /**
   * Return the gcb and cppcodegen passes.
   */
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
  public static List<Pass> lcb(LcbConfiguration configuration)
      throws IOException {
    List<Pass> passes = new ArrayList<>(gcbAndCppCodeGen(configuration));
    passes.add(new IsaMatchingPass(configuration));
    passes.add(new LlvmLoweringPass(configuration));
    passes.add(new vadl.lcb.clang.lib.Driver.ToolChains.EmitClangToolChainFilePass(configuration));
    passes.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetHeaderFilePass(configuration));
    passes.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetsFilePass(configuration));
    passes.add(new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetCppFilePass(configuration));
    passes.add(new vadl.lcb.template.clang.lib.Basic.EmitClangBasicCMakeFilePass(configuration));
    passes.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenModuleCMakeFilePass(configuration));
    passes.add(new vadl.lcb.template.clang.lib.CodeGen.Targets.EmitClangCodeGenTargetFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenTargetInfoHeaderFilePass(configuration));
    passes.add(new vadl.lcb.clang.lib.CodeGen.EmitCodeGenModuleFilePass(configuration));
    passes.add(new vadl.lcb.template.lld.ELF.EmitLldDriverFilePass(configuration));
    passes.add(new vadl.lcb.template.lld.ELF.EmitLldELFCMakeFilePass(configuration));
    passes.add(new vadl.lcb.template.lld.ELF.EmitLldTargetHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldManualEncodingHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lld.ELF.Arch.EmitImmediateUtilsHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lld.ELF.Arch.EmitLldArchFilePass(configuration));
    passes.add(new vadl.lcb.template.lld.ELF.EmitLldTargetCppFilePass(configuration));
    passes.add(new vadl.lcb.template.EmitLcbMakeFilePass(configuration));
    passes.add(new vadl.lcb.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass(
        configuration));
    passes.add(new vadl.lcb.include.llvm.BinaryFormat.EmitElfHeaderFilePass(configuration));
    passes.add(new vadl.lcb.include.llvm.Object.EmitELFObjectHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Misc.EmitBenchmarkRegisterHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitFrameLoweringCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitMachineFunctionInfoHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetObjectFileCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitInstrInfoHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitExpandPseudoHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitDAGToDAGIselHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserHeaderFilePass(
            configuration));
    passes.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCMakeFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserCppFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.EmitDAGToDAGISelCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitAsmPrinterHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitCallingConvTableGenFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.Utils.EmitBaseInfoFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetTableGenFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitAsmPrinterCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitSubTargetHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitFrameLoweringHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitPassConfigHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCMakeFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitISelLoweringCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCMakeFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCppFile(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitPassConfigCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitSubTargetCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetCMakeFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterCppFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprHeaderFilePass(configuration));
    passes.add(new EmitMCInstLowerCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCMakeFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstrPrinterHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitTargetStreamerHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoHeaderFilePass(configuration));
    passes.add(new EmitMCInstLowerHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterHeaderFilePass(
        configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitFixupKindsHeaderFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstrPrinterCppFilePass(configuration));
    passes.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderHeaderFilePass(
        configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitInstrInfoCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetMachineCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetMachineHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitExpandPseudoCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitTargetObjectFileHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Target.EmitISelLoweringHeaderFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.TargetParser.EmitTripleCppFilePass(configuration));
    passes.add(new vadl.lcb.template.lib.Object.EmitElfCppFilePass(configuration));

    return passes;
  }
}
