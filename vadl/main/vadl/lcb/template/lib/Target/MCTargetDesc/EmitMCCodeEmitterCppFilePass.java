package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.lcb.template.utils.ImmediateEncodingFunctionProvider.generateEncodeFunctions;
import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

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

  record InstructionUpdate(Instruction instruction, int opNo) {

  }

  record FixupUpdate(Fixup fixup,
                     List<InstructionUpdate> instructionUpdates) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var immediates = generateImmediates(passResults);
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var tableGenMachineInstructions = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var relocationPerFormat = output.relocationPerFormat();

    var fixups =
        relocationPerFormat.entrySet()
            .stream()
            .flatMap(x -> x.getValue().stream())
            .map(compilerRelocation -> {
              var fixup = ensureNonNull(output.fixupPerCompilerRelocation().get(compilerRelocation),
                  "fixup must exist");
              var instructionsAffected =
                  ensureNonNull(output.instructionsPerCompilerRelocation().get(compilerRelocation),
                      "instructions must exist");
              return new FixupUpdate(fixup,
                  instructionUpdates(tableGenMachineInstructions,
                      instructionsAffected,
                      compilerRelocation));
            })
            .filter(x -> !x.instructionUpdates.isEmpty())
            .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "immediates", immediates,
        "fixups", fixups);
  }

  private List<InstructionUpdate> instructionUpdates(
      List<TableGenMachineInstruction> machineInstructions, List<Instruction> instructions,
      CompilerRelocation compilerRelocation) {
    var updates = new ArrayList<InstructionUpdate>();

    for (var instruction : instructions) {
      var index =
          getIndexOfOperand(machineInstructions, instruction, compilerRelocation);

      index.ifPresent(integer -> updates.add(new InstructionUpdate(instruction, integer)));
    }

    return updates;
  }

  private Optional<Integer> getIndexOfOperand(List<TableGenMachineInstruction> machineInstructions,
                                              Instruction instruction,
                                              CompilerRelocation compilerRelocation) {
    var tableGenMachineInstruction =
        machineInstructions.stream()
            .filter(x -> x.instruction() == instruction)
            .findFirst();

    if (tableGenMachineInstruction.isEmpty()) {
      return Optional.empty();
    }

    for (int i = 0; i < tableGenMachineInstruction.get().getInOperands().size(); i++) {
      var operand = tableGenMachineInstruction.get().getInOperands().get(i);

      var isFieldAccess = operand.origin() instanceof LlvmFieldAccessRefNode fieldAccessRefNode
          && fieldAccessRefNode.immediateOperand().fieldAccessRef().fieldRef() ==
          compilerRelocation.immediate();
      var isFieldRef = operand.origin() instanceof FieldRefNode fieldRefNode
          && fieldRefNode.formatField() == compilerRelocation.immediate();
      var isLabel = operand.origin() instanceof LlvmBasicBlockSD basicBlockSD
          && basicBlockSD.fieldAccess().fieldRef() == compilerRelocation.immediate();

      if (isFieldAccess || isFieldRef || isLabel) {
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
