// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.DetermineBuiltinAttributesPass;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.gcb.passes.DetermineRelocationTypeForFieldPass;
import vadl.gcb.passes.GenerateCompilerRegistersPass;
import vadl.gcb.passes.GenerateGcbIntrinsicsPass;
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.InstructionPatternPruningPass;
import vadl.gcb.passes.PredicateFunctionInlinerPass;
import vadl.gcb.passes.RenamingConflictingRegistersPass;
import vadl.gcb.passes.SetMissingConfigurationValuesPass;
import vadl.gcb.passes.assembly.AssemblyConcatBuiltinMergingPass;
import vadl.gcb.passes.encodingGeneration.GenerateFieldAccessEncodingAndPredicateFunctionsPass;
import vadl.gcb.passes.operands.GenerateInstructionOperandsPass;
import vadl.lcb.codegen.assembly.WrapInIntegralPass;
import vadl.lcb.passes.OverwriteInputOperandsPass;
import vadl.lcb.passes.asm.AsmGrammarRuleGenerationPass;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenAbiSequenceInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.ISelLoweringOperationActionPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.RemoveRegisterWritesPass;
import vadl.lcb.passes.llvmLowering.RemoveTruncationAndAnyExtPass;
import vadl.lcb.passes.llvmLowering.compensation.CompensationPatternPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.pseudo.AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass;
import vadl.lcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.EmitLcbMakeFilePass;
import vadl.lcb.template.clang.include.Basic.EmitBuiltinsTableGenPass;
import vadl.lcb.template.clang.include.Basic.EmitCMakeListsPass;
import vadl.lcb.template.clang.include.Basic.EmitTargetBuiltinsHeaderPass;
import vadl.lcb.template.clang.lib.Basic.EmitClangBasicCMakeFilePass;
import vadl.lcb.template.clang.lib.Basic.Targets.EmitClangTargetCppFilePass;
import vadl.lcb.template.clang.lib.Basic.Targets.EmitClangTargetHeaderFilePass;
import vadl.lcb.template.clang.lib.Basic.Targets.EmitClangTargetsFilePass;
import vadl.lcb.template.clang.lib.CodeGen.EmitCGBuiltinFilePass;
import vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenFunctionHeaderFilePass;
import vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenModuleCMakeFilePass;
import vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenTargetInfoHeaderFilePass;
import vadl.lcb.template.clang.lib.CodeGen.Targets.EmitClangCodeGenTargetFilePass;
import vadl.lcb.template.clang.lib.Driver.ToolChains.EmitClangCommonArgsToolChainFilePass;
import vadl.lcb.template.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass;
import vadl.lcb.template.include.llvm.TargetParser.EmitTripleHeaderFilePass;
import vadl.lcb.template.lib.IR.EmitMiddleendMainIntrinsicsTableGenPass;
import vadl.lcb.template.lib.Misc.EmitBenchmarkRegisterHeaderFilePass;
import vadl.lcb.template.lib.Object.EmitElfCppFilePass;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandCppFilePass;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandHeaderFilePass;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCMakeFilePass;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCppFilePass;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserCppFilePass;
import vadl.lcb.template.lib.Target.AsmParser.EmitAsmRecursiveDescentParserHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitAsmPrinterCppFilePass;
import vadl.lcb.template.lib.Target.EmitAsmPrinterHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitCallingConvTableGenFilePass;
import vadl.lcb.template.lib.Target.EmitDAGToDAGISelCppFilePass;
import vadl.lcb.template.lib.Target.EmitDAGToDAGIselHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitFrameLoweringCppFilePass;
import vadl.lcb.template.lib.Target.EmitFrameLoweringHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitISelLoweringCppFilePass;
import vadl.lcb.template.lib.Target.EmitISelLoweringHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitInstrInfoCppFilePass;
import vadl.lcb.template.lib.Target.EmitInstrInfoHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitMachineFunctionInfoHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitPassConfigCppFilePass;
import vadl.lcb.template.lib.Target.EmitPassConfigHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass;
import vadl.lcb.template.lib.Target.EmitRegisterInfoHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass;
import vadl.lcb.template.lib.Target.EmitSubTargetCppFilePass;
import vadl.lcb.template.lib.Target.EmitSubTargetHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitTargetCMakeFilePass;
import vadl.lcb.template.lib.Target.EmitTargetHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitTargetMachineCppFilePass;
import vadl.lcb.template.lib.Target.EmitTargetMachineHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitTargetObjectFileCppFilePass;
import vadl.lcb.template.lib.Target.EmitTargetObjectFileHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitTargetTableGenFilePass;
import vadl.lcb.template.lib.Target.EmitTargetTransformInfoCppFilePass;
import vadl.lcb.template.lib.Target.EmitTargetTransformInfoHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitVadlBuiltinHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitVadlBuiltinHeaderPPFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitConstMatIntCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitConstMatIntHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitFixupKindsHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMcTargetDescCMakeFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitTargetStreamerHeaderFilePass;
import vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCMakeFilePass;
import vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCppFile;
import vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoHeaderFilePass;
import vadl.lcb.template.lib.Target.Utils.EmitBaseInfoFilePass;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.lcb.template.lib.TargetParser.EmitTripleCppFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitImmediateUtilsHeaderFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitLldArchFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitLldManualEncodingHeaderFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitLldTargetRelocationsHeaderFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitLldVadlBuiltinsHeaderFilePass;
import vadl.lcb.template.lld.ELF.EmitLldDriverFilePass;
import vadl.lcb.template.lld.ELF.EmitLldELFCMakeFilePass;
import vadl.lcb.template.lld.ELF.EmitLldTargetCppFilePass;
import vadl.lcb.template.lld.ELF.EmitLldTargetHeaderFilePass;
import vadl.pass.PassOrder;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.behaviorRewrite.BehaviorRewritePass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.functionInliner.ArtificialResInlinerPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.statusBuiltInInlinePass.RemoveUnusedStatusFlagsFromBuiltinsPass;
import vadl.viam.passes.statusBuiltInInlinePass.StatusBuiltInInlinePass;

/**
 * LCB and GCB-specific pipeline extensions.
 */
public final class LcbGcbPassOrders {
  private LcbGcbPassOrders() {
  }

  public static PassOrder gcbAndCppCodeGen(GcbConfiguration config) throws IOException {
    return extendGcbAndCppCodeGen(ViamPassOrders.viam(config), config);
  }

  public static PassOrder lcb(LcbConfiguration config) throws IOException {
    return extendLcb(gcbAndCppCodeGen(config), config);
  }

  /**
   * Extends a shared VIAM pipeline with GCB lowering and shared C/C++ generation passes.
   */
  public static PassOrder extendGcbAndCppCodeGen(PassOrder order, GcbConfiguration config) {
    order.add(new SetMissingConfigurationValuesPass(config));
    order.skip(StatusBuiltInInlinePass.class);
    order.skip(ArtificialResInlinerPass.class);

    order.add(new GenerateCompilerRegistersPass(config));
    order.skip(FieldAccessInlinerPass.class);
    order.add(new IdentifyFieldUsagePass(config));
    order.add(new DetermineRelocationTypeForFieldPass(config));
    order.add(new GenerateValueRangeImmediatePass(config));
    order.add(new GenerateFieldAccessEncodingAndPredicateFunctionsPass(config));
    order.add(new PredicateFunctionInlinerPass(config));
    order.add(new AssemblyConcatBuiltinMergingPass(config));
    order.add(new DetermineRegisterUsesAndDefsPass(config));
    order.add(new GenerateInstructionOperandsPass(config));
    order.add(new InstructionPatternPruningPass(config));
    order.add(new DetermineBuiltinAttributesPass(config));
    order.add(new GenerateGcbIntrinsicsPass(config));

    return PassOrderPipelineUtils.addHtmlDump(order, config, "gcbProcessing",
        "Now the gcb produced all necessary encoding function for field accesses "
            + "and normalized VIAM types to Cpp types.");
  }

  /**
   * Extends a GCB pipeline with LCB lowering and LLVM/Clang/LLD emission passes.
   */
  public static PassOrder extendLcb(PassOrder order, LcbConfiguration config) throws IOException {
    order.skip(FieldAccessInlinerPass.class);

    order.add(new PseudoExpansionFunctionGeneratorPass(config));
    order.add(new AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass(config));
    order.add(new RemoveTruncationAndAnyExtPass(config));
    order.add(new GenerateTableGenRegistersPass(config));
    order.add(new RemoveRegisterWritesPass(config));
    order.add(new RemoveUnusedStatusFlagsFromBuiltinsPass(config));
    order.add(new CanonicalizationPass(config));
    order.add(new AlgebraicSimplificationPass(config));
    order.add(new BehaviorRewritePass(config));
    order.add(new IsaMachineInstructionMatchingPass(config));
    order.add(new IsaPseudoInstructionMatchingPass(config));
    order.add(new LlvmLoweringPass(config));
    order.add(new GenerateTableGenMachineInstructionRecordPass(config));
    order.add(new GenerateTableGenPseudoInstructionRecordPass(config));
    order.add(new GenerateTableGenAbiSequenceInstructionRecordPass(config));
    order.add(new GenerateTableGenImmediateRecordPass(config));
    order.add(new CreateFunctionsFromImmediatesPass(config));
    order.add(new CompensationPatternPass(config));
    order.add(new ISelLoweringOperationActionPass(config));
    order.add(new GenerateLinkerComponentsPass(config));
    order.add(new WrapInIntegralPass(config));
    order.add(new AsmGrammarRuleGenerationPass(config));

    PassOrderPipelineUtils.addHtmlDump(order, config, "lcbLlvmLowering",
        "The LCB did ISA matching to and lowered common VIAM nodes to LLVM specific"
            + "nodes.");

    order.add(new EmitVadlBuiltinHeaderFilePass(config));
    order.add(new EmitVadlBuiltinHeaderPPFilePass(config));
    order.add(new EmitCMakeListsPass(config));
    order.add(new EmitBuiltinsTableGenPass(config));
    order.add(new EmitTargetBuiltinsHeaderPass(config));
    order.add(new vadl.lcb.clang.lib.Driver.ToolChains.EmitClangToolChainFilePass(config));
    order.add(new EmitClangCommonArgsToolChainFilePass(config));
    order.add(new EmitClangTargetHeaderFilePass(config));
    order.add(new EmitClangTargetsFilePass(config));
    order.add(new EmitClangTargetCppFilePass(config));
    order.add(new EmitClangBasicCMakeFilePass(config));
    order.add(new EmitCodeGenModuleCMakeFilePass(config));
    order.add(new EmitClangCodeGenTargetFilePass(config));
    order.add(new EmitCodeGenTargetInfoHeaderFilePass(config));
    order.add(new vadl.lcb.clang.lib.CodeGen.EmitCodeGenModuleFilePass(config));
    order.add(new EmitLldDriverFilePass(config));
    order.add(new EmitLldELFCMakeFilePass(config));
    order.add(new EmitLldTargetHeaderFilePass(config));
    order.add(new EmitLldTargetRelocationsHeaderFilePass(config));
    order.add(new EmitLldManualEncodingHeaderFilePass(config));
    order.add(new EmitImmediateUtilsHeaderFilePass(config));
    order.add(new EmitLldArchFilePass(config));
    order.add(new EmitLldVadlBuiltinsHeaderFilePass(config));
    order.add(new EmitLldTargetCppFilePass(config));
    order.add(new EmitLcbMakeFilePass(config));
    order.add(new EmitTargetElfRelocsDefFilePass(config));
    order.add(new vadl.lcb.include.llvm.IR.EmitCMakeListsPass(config));
    order.add(new EmitMiddleendMainIntrinsicsTableGenPass(config));
    order.add(new vadl.lcb.include.llvm.IR.EmitMiddleendIntrinsicsTableGenPass(config));
    order.add(new EmitCGBuiltinFilePass(config));
    order.add(new EmitCodeGenFunctionHeaderFilePass(config));
    order.add(new vadl.lcb.include.llvm.BinaryFormat.EmitElfHeaderFilePass(config));
    order.add(new vadl.lcb.include.llvm.Object.EmitELFObjectHeaderFilePass(config));
    order.add(new EmitBenchmarkRegisterHeaderFilePass(config));
    order.add(new EmitFrameLoweringCppFilePass(config));
    order.add(new EmitMachineFunctionInfoHeaderFilePass(config));
    order.add(new EmitTargetObjectFileCppFilePass(config));
    order.add(new EmitInstrInfoHeaderFilePass(config));
    order.add(new EmitDAGToDAGIselHeaderFilePass(config));
    order.add(new EmitAsmParsedOperandCppFilePass(config));
    order.add(new EmitAsmParsedOperandHeaderFilePass(config));
    order.add(new EmitAsmRecursiveDescentParserHeaderFilePass(config));
    order.add(new EmitAsmParserCppFilePass(config));
    order.add(new EmitAsmParserCMakeFilePass(config));
    order.add(new EmitAsmRecursiveDescentParserCppFilePass(config));
    order.add(new EmitDAGToDAGISelCppFilePass(config));
    order.add(new EmitAsmPrinterHeaderFilePass(config));
    order.add(new EmitCallingConvTableGenFilePass(config));
    order.add(new EmitRegisterInfoHeaderFilePass(config));
    order.add(new EmitBaseInfoFilePass(config));
    order.add(new EmitImmediateFilePass(config));
    order.add(new EmitTargetTableGenFilePass(config));
    order.add(new EmitTargetHeaderFilePass(config));
    order.add(new EmitAsmPrinterCppFilePass(config));
    order.add(new EmitSubTargetHeaderFilePass(config));
    order.add(new EmitFrameLoweringHeaderFilePass(config));
    order.add(new EmitPassConfigHeaderFilePass(config));
    order.add(new EmitISelLoweringCppFilePass(config));
    order.add(new EmitTargetInfoHeaderFilePass(config));
    order.add(new EmitTargetInfoCMakeFilePass(config));
    order.add(new EmitTargetInfoCppFile(config));
    order.add(new EmitPassConfigCppFilePass(config));
    order.add(new EmitSubTargetCppFilePass(config));
    order.add(new EmitTargetCMakeFilePass(config));
    order.add(new EmitMCCodeEmitterHeaderFilePass(config));
    order.add(new EmitMcTargetDescCMakeFilePass(config));
    order.add(new EmitMCCodeEmitterCppFilePass(config));
    order.add(new EmitAsmStreamerCppFilePass(config));
    order.add(new EmitELFStreamerCppFilePass(config));
    order.add(new EmitConstMatIntHeaderFilePass(config));
    order.add(new EmitConstMatIntCppFilePass(config));
    order.add(new EmitMCInstExpanderCppFilePass(config));
    order.add(new EmitAsmBackendHeaderFilePass(config));
    order.add(new EmitELFObjectWriterCppFilePass(config));
    order.add(new EmitMCExprHeaderFilePass(config));
    order.add(new EmitMCInstLowerCppFilePass(config));
    order.add(new EmitMCExprCppFilePass(config));
    order.add(new EmitMCTargetDescHeaderFilePass(config));
    order.add(new EmitAsmUtilsCppFilePass(config));
    order.add(new EmitMCTargetDescCppFilePass(config));
    order.add(new EmitInstPrinterHeaderFilePass(config));
    order.add(new EmitAsmBackendCppFilePass(config));
    order.add(new EmitMCAsmInfoCppFilePass(config));
    order.add(new EmitELFStreamerHeaderFilePass(config));
    order.add(new EmitAsmStreamerHeaderFilePass(config));
    order.add(new EmitTargetStreamerHeaderFilePass(config));
    order.add(new EmitMCAsmInfoHeaderFilePass(config));
    order.add(new EmitMCInstLowerHeaderFilePass(config));
    order.add(new EmitELFObjectWriterHeaderFilePass(config));
    order.add(new EmitFixupKindsHeaderFilePass(config));
    order.add(new EmitInstPrinterCppFilePass(config));
    order.add(new EmitAsmUtilsHeaderFilePass(config));
    order.add(new EmitMCInstExpanderHeaderFilePass(config));
    order.add(new EmitRegisterInfoTableGenFilePass(config));
    order.add(new EmitInstrInfoCppFilePass(config));
    order.add(new EmitInstrInfoTableGenFilePass(config));
    order.add(new EmitRegisterInfoCppFilePass(config));
    order.add(new EmitTargetMachineCppFilePass(config));
    order.add(new EmitTargetMachineHeaderFilePass(config));
    order.add(new EmitTargetObjectFileHeaderFilePass(config));
    order.add(new EmitISelLoweringHeaderFilePass(config));
    order.add(new EmitTargetTransformInfoHeaderFilePass(config));
    order.add(new EmitTargetTransformInfoCppFilePass(config));
    order.add(new EmitTripleHeaderFilePass(config));
    order.add(new EmitTripleCppFilePass(config));
    order.add(new EmitElfCppFilePass(config));

    return order;
  }
}
