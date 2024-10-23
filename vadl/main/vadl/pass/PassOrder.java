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
import vadl.gcb.passes.pseudo.PseudoInstructionArgumentReplacementPass;
import vadl.gcb.passes.relocation.IdentifyFieldUsagePass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
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
import vadl.template.AbstractTemplateRenderingPass;
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
 * This class defines the order in which the {@link PassManager} should run them.
 */
public final class PassOrder {

  // a counter-map that keeps track of how many passes of each pass class exists.
  // this is used to generate a unique pass key if it is not given by the user.
  private static final Map<Class<? extends Pass>, Integer> passCounter
      = new ConcurrentHashMap<>();

  // the actual list of pass steps
  @SuppressWarnings("JdkObsolete")
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

}
