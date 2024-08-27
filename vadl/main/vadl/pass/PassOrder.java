package vadl.pass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPassForDecoding;
import vadl.dump.HtmlDumpPass;
import vadl.error.VadlError;
import vadl.error.VadlException;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.dummyAbi.DummyAbiPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * This class defines the order in which the {@link PassManager} should run them.
 */
public final class PassOrder {

  // a counter-map that keeps track of how many passes of each pass class exists.
  // this is used to generate a unique pass key if it is not given by the user.
  private static final Map<Class<? extends Pass>, Integer> passCounter
      = new ConcurrentHashMap<>();

  // the actual list of pass steps
  private List<PassStep> order = new ArrayList<>();

  /**
   * Add a pass to the pass order. If the passKey is null, it will generate a unique one.
   *
   * @return this
   */
  public PassOrder add(@Nullable PassKey passKey, Pass pass) {
    var currentId = passCounter.merge(pass.getClass(), 1, Integer::sum);
    if (passKey == null) {
      passKey = new PassKey(pass.getClass().getName() + "-" + currentId);
    }
    order.add(new PassStep(passKey, pass));
    return this;
  }

  /**
   * Add a pass to the pass order.
   *
   * @return this
   */
  public PassOrder add(String key, Pass pass) {
    add(new PassKey(key), pass);
    return this;
  }

  /**
   * Add a pass to the pass order. The key will be generated.
   *
   * @return this
   */
  public PassOrder add(Pass pass) {
    add((PassKey) null, pass);
    return this;
  }

  /**
   * Get the list of pass steps in this pass order.
   */
  public List<PassStep> passSteps() {
    return order;
  }

  public PassOrder untilFirst(Class<? extends Pass> passClass) {
    var instance = order.stream().filter(s -> passClass.isInstance(s.pass()))
        .findFirst()
        .get();
    var indexOf = order.indexOf(instance);
    order.subList(indexOf + 1, order.size()).clear();
    return this;
  }


  /**
   * Return the viam passes.
   */
  public static PassOrder viam(GeneralConfiguration configuration) throws IOException {
    var order = new PassOrder();

    order.add(new ViamVerificationPass(configuration));
    order.add(new DummyAbiPass(configuration));

    order.add(new TypeCastEliminationPass(configuration));
    order.add(new FunctionInlinerPass(configuration));
    order.add(new SideEffectConditionResolvingPass(configuration));

    order.add(new CanonicalizationPass(configuration));
    order.add(new AlgebraicSimplificationPass(configuration));

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

    order.add(new GenerateFieldAccessEncodingFunctionPass(gcbConfiguration));
    order.add(new FieldNodeReplacementPassForDecoding(gcbConfiguration));
    order.add(new CppTypeNormalizationForEncodingsPass(gcbConfiguration));
    order.add(new CppTypeNormalizationForDecodingsPass(gcbConfiguration));
    order.add(new CppTypeNormalizationForPredicatesPass(gcbConfiguration));

    if (gcbConfiguration.doDump()) {
      var config = HtmlDumpPass.Config.from(gcbConfiguration,
          "gcbProcessing",
          // TODO: @kper more meaningful description on what actual happened since the last
          //   HTML dump.
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
    order.add(new LlvmLoweringPass(configuration));

    if (configuration.doDump()) {
      var config = HtmlDumpPass.Config.from(
          configuration,
          "lcbLlvmLowering",
          // TODO: @kper more meaningful description on what actual happened since the last
          //   HTML dump.
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
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCMakeFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitAsmUtilsCppFilePass(configuration));
    order.add(
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCTargetDescCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstrPrinterHeaderFilePass(
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
        new vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstrPrinterCppFilePass(configuration));
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
    order.add(new vadl.lcb.template.lib.TargetParser.EmitTripleCppFilePass(configuration));
    order.add(new vadl.lcb.template.lib.Object.EmitElfCppFilePass(configuration));

    return order;
  }
}
