package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.lcb.template.utils.ImmediateEncodingFunctionProvider.generateEncodeFunctions;
import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.VariantKind;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * This file contains the logic for emitting MC instructions.
 */
public class EmitMCCodeEmitterCppFilePass extends LcbTemplateRenderingPass {

  public EmitMCCodeEmitterCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCCodeEmitter.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCCodeEmitter.cpp";
  }

  /**
   * The LLVM's encoder/decoder does not interact with the {@code uint64_t decode(uint64_t)}
   * functions but with {@code unsigned decode(const MCInst InstMI, ...} from the MCCodeEmitter.
   * This {@code WRAPPER} is just the magic suffix for the
   * function.
   */
  public static final String WRAPPER = "wrapper";

  record Aggregate(String encodeWrapper, String encode) {

  }

  record TargetVariantUpdate(VariantKind kind, List<TargetInstructionUpdate> instructions) {

  }

  record TargetInstructionUpdate(Instruction instruction, Fixup fixup, int opNo) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var immediates = generateImmediates(passResults);
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var tableGenMachineInstructions = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);

    var result = new ArrayList<TargetVariantUpdate>();
    for (var variantKind : output.variantKinds()) {
      var result2 = new ArrayList<TargetInstructionUpdate>();
      for (var fixup : output.fixups()) {
        if (fixup.variantKind().equals(variantKind)) {
          var instructionsAffected =
              output.instructionsPerCompilerRelocation()
                  .get((CompilerRelocation) fixup.relocationLowerable());

          // If null then immediate
          if (instructionsAffected != null) {
            var update = instructionUpdates(
                fixup,
                tableGenMachineInstructions,
                instructionsAffected,
                (CompilerRelocation) fixup.relocationLowerable());
            result2.addAll(update);
          }
        }
      }
      result.add(new TargetVariantUpdate(variantKind, result2));
    }

    var resultSym = new ArrayList<TargetInstructionUpdate>();
    for (var instruction : output.instructionPerImmediateVariant().values().stream()
        .flatMap(Collection::stream).toList()) {
      var format = instruction.format();
      var imms = fieldUsages.getImmediates(format);
      var touchesPc = instruction.behavior().getNodes(WriteRegNode.class)
          .anyMatch(x -> x.staticCounterAccess() != null);

      for (var imm : imms) {
        var index = getIndexOfOperand(tableGenMachineInstructions, instruction, imm);
        var fixupCandidates = output.fixupsByField().get(imm);
        ensureNonNull(fixupCandidates, "fixups must exist");

        // If it touches PC then emit only PC-relative.
        var needle =
            touchesPc ? CompilerRelocation.Kind.RELATIVE : CompilerRelocation.Kind.ABSOLUTE;
        fixupCandidates.stream().filter(x -> x.kind() == needle)
            .forEach(fixup -> index.ifPresent(
                i -> resultSym.add(new TargetInstructionUpdate(instruction, fixup, i))));
      }
    }

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "immediates", immediates,
        "variantUpdates", result,
        "syms", resultSym);
  }

  private List<TargetInstructionUpdate> instructionUpdates(
      Fixup fixup,
      List<TableGenMachineInstruction> machineInstructions, List<Instruction> instructions,
      CompilerRelocation compilerRelocation) {
    var updates = new ArrayList<TargetInstructionUpdate>();

    for (var instruction : instructions) {
      var index =
          getIndexOfOperand(machineInstructions, instruction, compilerRelocation.immediate());

      index.ifPresent(
          integer -> updates.add(new TargetInstructionUpdate(instruction, fixup, integer)));
    }

    return updates;
  }

  private Optional<Integer> getIndexOfOperand(List<TableGenMachineInstruction> machineInstructions,
                                              Instruction instruction,
                                              Format.Field field) {
    var tableGenMachineInstruction =
        machineInstructions.stream()
            .filter(x -> x.instruction() == instruction)
            .findFirst();

    if (tableGenMachineInstruction.isEmpty()) {
      return Optional.empty();
    }

    for (int i = 0; i < tableGenMachineInstruction.get().getInOperands().size(); i++) {
      var operand = tableGenMachineInstruction.get().getInOperands().get(i);
      var matches = GenerateLinkerComponentsPass.operandMatchesImmediate(field, operand);

      if (matches) {
        return Optional.of(i);
      }
    }

    throw Diagnostic.error("There is no immediate operand for this machine instruction",
        instruction.sourceLocation()).build();
  }

  private List<Aggregate> generateImmediates(PassResults passResults) {
    return generateEncodeFunctions(passResults)
        .values()
        .stream()
        .map(f -> new Aggregate(f.identifier.append(WRAPPER).lower(), f.identifier.lower()))
        .toList();
  }
}
