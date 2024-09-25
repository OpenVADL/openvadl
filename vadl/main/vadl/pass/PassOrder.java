package vadl.pass;

import static vadl.iss.template.IssDefaultRenderingPass.issDefault;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.passes.fieldNodeReplacement.FieldNodeReplacementPassForDecoding;
import vadl.dump.HtmlDumpPass;
import vadl.gcb.passes.assembly.AssemblyConcatBuiltinMergingPass;
import vadl.gcb.passes.assembly.AssemblyReplacementNodePass;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.GenerateLogicalRelocationPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.iss.passes.IssConfigurationPass;
import vadl.iss.passes.IssVerificationPass;
import vadl.iss.template.target.EmitIssCpuHeaderPass;
import vadl.iss.template.target.EmitIssCpuParamHeaderPass;
import vadl.iss.template.target.EmitIssCpuQomHeaderPass;
import vadl.iss.template.target.EmitIssCpuSourcePass;
import vadl.iss.template.target.EmitIssTranslatePass;
import vadl.lcb.codegen.GenerateImmediateKindPass;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerCppFilePass;
import vadl.lcb.template.lib.Target.EmitMCInstLowerHeaderFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitInstPrinterHeaderFilePass;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.dummyAbi.DummyAbiPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass;
import vadl.viam.passes.staticCounterAccess.StaticCounterAccessResolvingPass;
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
  private final LinkedList<PassStep> order = new LinkedList<>();

  /**
   * Add a pass to the pass order. If the passKey is null, it will generate a unique one.
   *
   * @return this
   */
  public PassOrder add(@Nullable PassKey passKey, Pass pass) {
    order.add(createPassStep(passKey, pass));
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

  /**
   * Adds a given pass after the pass with the given {@code passName}.
   */
  public PassOrder addAfterLast(Class<?> passName, Pass pass) {
    int index = -1;
    for (int i = 0; i < passSteps().size(); i++) {
      if (passName.isInstance(passSteps().get(i).pass())) {
        index = i;
      }
    }

    if (index != -1) {
      var step = createPassStep(null, pass);
      passSteps().add(index + 1, step);
    }

    return this;
  }

  /**
   * Truncates the PassOrder to only include passes until (including) the first
   * instance of the given pass class.
   * This is helpful for tests to avoid executing more passes than necessary.
   */
  public PassOrder untilFirst(Class<? extends Pass> passClass) {
    var instance = order.stream().filter(s -> passClass.isInstance(s.pass()))
        .findFirst()
        .get();
    var indexOf = order.indexOf(instance);
    order.subList(indexOf + 1, order.size()).clear();
    return this;
  }

  /**
   * Injects a dump pass between each existing pass of this pass order.
   * TemplateRenderingPasses and {@link ViamVerificationPass} are not affected by this
   * method.
   * It is most useful for debugging, as it allows to inspect the VIAM's state after every
   * executed pass.
   */
  public PassOrder dumpAfterEach(String outPath) {
    var config = new GeneralConfiguration(Path.of(outPath), true);
    // We use a ListIterator for safe modification while iterating
    var iterator = order.listIterator();

    while (iterator.hasNext()) {
      var currentPass = iterator.next();
      if (currentPass.pass() instanceof AbstractTemplateRenderingPass
          || currentPass.pass() instanceof ViamVerificationPass
      ) {
        // do not dump renderings or verifications
        continue;
      }

      HtmlDumpPass dumpPass = new HtmlDumpPass(HtmlDumpPass.Config.from(config,
          currentPass.pass().getName().value(),
          "This is a dump right after the pass " + currentPass.key().value() + "."
      ));

      // Check if there is a next element to decide where to add the dump pass
      if (iterator.hasNext()) {
        iterator.add(createPassStep(null, dumpPass));
      } else {
        // If at the end, also add the dump pass
        iterator.add(createPassStep(null, dumpPass));
        break; // Break after adding at the end to avoid infinite loop
      }
    }
    return this;
  }

  /**
   * Adds a dump pass that outputs the dump to the given path.
   */
  public PassOrder addDump(String outPath) {
    var config = new GeneralConfiguration(Path.of(outPath), true);
    var last = order.getLast();
    HtmlDumpPass dumpPass = new HtmlDumpPass(HtmlDumpPass.Config.from(config,
        last.pass().getName().value(),
        "This is a dump right after the pass " + last.key().value() + "."
    ));
    add(dumpPass);
    return this;
  }

  private PassStep createPassStep(@Nullable PassKey passKey, Pass pass) {
    var currentId = passCounter.merge(pass.getClass(), 1, Integer::sum);
    if (passKey == null) {
      passKey = new PassKey(pass.getClass().getName() + "-" + currentId);
    }
    return new PassStep(passKey, pass);
  }


  /**
   * Return the viam passes.
   */
  public static PassOrder viam(GeneralConfiguration configuration) throws IOException {
    var order = new PassOrder();

    order.add(new ViamVerificationPass(configuration));
    order.add(new DummyAbiPass(configuration));

    order.add(new TypeCastEliminationPass(configuration));
    // TODO: @kper do you see any fix for this?
    // Note: we run the counter-access resolving pass before the func inliner pass
    // because the lcb uses the unlinined version of the instructions.
    // However, this might miss a lot of opportunities to statically resolve counter-accesses
    // as the canicalization runs at a later point.
    order.add(new StaticCounterAccessResolvingPass(configuration));
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
    order.add(new AssemblyReplacementNodePass(gcbConfiguration));
    order.add(new AssemblyConcatBuiltinMergingPass(gcbConfiguration));
    order.add(new DetectImmediatePass(gcbConfiguration));
    order.add(new GenerateLogicalRelocationPass(gcbConfiguration));
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
    order.add(new GenerateImmediateKindPass(configuration));
    order.add(new IsaMatchingPass(configuration));
    order.add(new GenerateRegisterClassesPass(configuration));
    order.add(new LlvmLoweringPass(configuration));
    order.add(new GenerateElfRelocationPass(configuration));

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
        .add(new IssConfigurationPass(config));

    if (config.doDump()) {
      order.add(new HtmlDumpPass(HtmlDumpPass.Config.from(config, "ISS Generation Dump", """
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
        // target/gen-arch/cpu-qom.h
        .add(new EmitIssCpuQomHeaderPass(config))
        // target/gen-arch/cpu-param.h
        .add(new EmitIssCpuParamHeaderPass(config))
        // target/gen-arch/cpu.h
        .add(new EmitIssCpuHeaderPass(config))
        // target/gen-arch/cpu.c
        .add(new EmitIssCpuSourcePass(config))
        // target/gen-arch/translate.c
        .add(new EmitIssTranslatePass(config))
    ;
  }

}
