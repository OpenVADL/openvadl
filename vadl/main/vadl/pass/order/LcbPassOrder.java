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

import java.io.IOException;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.assembly.WrapInIntegralPass;
import vadl.lcb.include.llvm.IR.EmitMiddleendIntrinsicsTableGenPass;
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
import vadl.lcb.template.clang.lib.Basic.Targets.EmitClangTargetHeaderFilePass;
import vadl.lcb.template.clang.lib.CodeGen.EmitCGBuiltinFilePass;
import vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenFunctionHeaderFilePass;
import vadl.lcb.template.include.llvm.BinaryFormat.ELFRelocs.EmitTargetElfRelocsDefFilePass;
import vadl.lcb.template.lib.IR.EmitMiddleendMainIntrinsicsTableGenPass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitVadlBuiltinHeaderFilePass;
import vadl.lcb.template.lib.Target.EmitVadlBuiltinHeaderPPFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitConstMatIntCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitConstMatIntHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterHeaderFilePass;
import vadl.lcb.template.lld.ELF.Arch.EmitLldVadlBuiltinsHeaderFilePass;
import vadl.pass.PassOrder;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.behaviorRewrite.BehaviorRewritePass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.statusBuiltInInlinePass.RemoveUnusedStatusFlagsFromBuiltinsPass;

/**
 * Builds the pass order used for LLVM/LCB generation.
 */
public final class LcbPassOrder {
  private LcbPassOrder() {
  }

  /**
   * Creates the pass order.
   */
  public static PassOrder create(LcbConfiguration configuration) throws IOException {
    var order = GcbPassOrder.create(configuration);
    order.skip(FieldAccessInlinerPass.class);

    order.add(new PseudoExpansionFunctionGeneratorPass(configuration));
    order.add(new AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass(configuration));
    order.add(new RemoveTruncationAndAnyExtPass(configuration));
    order.add(new GenerateTableGenRegistersPass(configuration));
    order.add(new RemoveRegisterWritesPass(configuration));
    order.add(new RemoveUnusedStatusFlagsFromBuiltinsPass(configuration));
    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));
    order.add(new BehaviorRewritePass(configuration));
    order.add(new IsaMachineInstructionMatchingPass(configuration));
    order.add(new IsaPseudoInstructionMatchingPass(configuration));
    order.add(new LlvmLoweringPass(configuration));
    order.add(new GenerateTableGenMachineInstructionRecordPass(configuration));
    order.add(new GenerateTableGenPseudoInstructionRecordPass(configuration));
    order.add(new GenerateTableGenAbiSequenceInstructionRecordPass(configuration));
    order.add(new GenerateTableGenImmediateRecordPass(configuration));
    order.add(new CreateFunctionsFromImmediatesPass(configuration));
    order.add(new CompensationPatternPass(configuration));
    order.add(new ISelLoweringOperationActionPass(configuration));
    order.add(new GenerateLinkerComponentsPass(configuration));
    order.add(new WrapInIntegralPass(configuration));
    order.add(new AsmGrammarRuleGenerationPass(configuration));

    OrderSupport.addHtmlDump(order, configuration,
        "lcbLlvmLowering",
        "The LCB did ISA matching to and lowered common VIAM nodes to LLVM specific"
            + "nodes.");

    order.add(new EmitVadlBuiltinHeaderFilePass(configuration));
    order.add(new EmitVadlBuiltinHeaderPPFilePass(configuration));
    order.add(new vadl.lcb.template.clang.include.Basic.EmitCMakeListsPass(configuration));
    order.add(new vadl.lcb.template.clang.include.Basic.EmitBuiltinsTableGenPass(configuration));
    order.add(
        new vadl.lcb.template.clang.include.Basic.EmitTargetBuiltinsHeaderPass(configuration));
    order.add(
        new vadl.lcb.clang.lib.Driver.ToolChains.EmitClangToolChainFilePass(configuration));
    order.add(new vadl.lcb.template.clang.lib.Driver.ToolChains
        .EmitClangCommonArgsToolChainFilePass(configuration));
    order.add(new EmitClangTargetHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.clang.lib.Basic.Targets.EmitClangTargetsFilePass(configuration));
    order.add(
        new vadl.lcb.template.clang.lib.Basic.Targets.EmitClangTargetCppFilePass(configuration));
    order.add(new vadl.lcb.template.clang.lib.Basic.EmitClangBasicCMakeFilePass(configuration));
    order.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenModuleCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.clang.lib.CodeGen.Targets
        .EmitClangCodeGenTargetFilePass(configuration));
    order.add(
        new vadl.lcb.template.clang.lib.CodeGen.EmitCodeGenTargetInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.clang.lib.CodeGen.EmitCodeGenModuleFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldDriverFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldELFCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldTargetHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.Arch
        .EmitLldTargetRelocationsHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitLldManualEncodingHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lld.ELF.Arch.EmitImmediateUtilsHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.Arch.EmitLldArchFilePass(configuration));
    order.add(new EmitLldVadlBuiltinsHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lld.ELF.EmitLldTargetCppFilePass(configuration));
    order.add(new vadl.lcb.template.EmitLcbMakeFilePass(configuration));
    order.add(new EmitTargetElfRelocsDefFilePass(configuration));
    order.add(new vadl.lcb.include.llvm.IR.EmitCMakeListsPass(configuration));
    order.add(new EmitMiddleendMainIntrinsicsTableGenPass(configuration));
    order.add(new EmitMiddleendIntrinsicsTableGenPass(configuration));
    order.add(new EmitCGBuiltinFilePass(configuration));
    order.add(new EmitCodeGenFunctionHeaderFilePass(configuration));
    order.add(new vadl.lcb.include.llvm.BinaryFormat.EmitElfHeaderFilePass(configuration));
    order.add(new vadl.lcb.include.llvm.Object.EmitELFObjectHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Misc.EmitBenchmarkRegisterHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitFrameLoweringCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.EmitMachineFunctionInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetObjectFileCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitInstrInfoHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitDAGToDAGIselHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParsedOperandCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser
        .EmitAsmParsedOperandHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser
        .EmitAsmRecursiveDescentParserHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser.EmitAsmParserCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.AsmParser
        .EmitAsmRecursiveDescentParserCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitDAGToDAGISelCppFilePass(configuration));
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
    order.add(new vadl.lcb.template.lib.Target.EmitISelLoweringCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCMakeFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.TargetInfo.EmitTargetInfoCppFile(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitPassConfigCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitSubTargetCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetCMakeFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterHeaderFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMcTargetDescCMakeFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerCppFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerCppFilePass(
            configuration));
    order.add(new EmitConstMatIntHeaderFilePass(configuration));
    order.add(new EmitConstMatIntCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCInstExpanderCppFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendHeaderFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFObjectWriterCppFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprHeaderFilePass(configuration));
    order.add(new EmitMCInstLowerCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCExprCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescHeaderFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCppFilePass(configuration));
    order.add(new EmitInstPrinterHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmBackendCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitELFStreamerHeaderFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmStreamerHeaderFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitTargetStreamerHeaderFilePass(
            configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCAsmInfoHeaderFilePass(
            configuration));
    order.add(new EmitMCInstLowerHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc
        .EmitELFObjectWriterHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitFixupKindsHeaderFilePass(
            configuration));
    order.add(new EmitInstPrinterCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc
        .EmitMCInstExpanderHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitInstrInfoCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetMachineCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetMachineHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitTargetObjectFileHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.EmitISelLoweringHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.EmitTargetTransformInfoHeaderFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.EmitTargetTransformInfoCppFilePass(configuration));
    order.add(new vadl.lcb.template.include.llvm.TargetParser
        .EmitTripleHeaderFilePass(configuration));
    order.add(new vadl.lcb.template.lib.TargetParser.EmitTripleCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Object.EmitElfCppFilePass(configuration));
    return order;
  }
}
