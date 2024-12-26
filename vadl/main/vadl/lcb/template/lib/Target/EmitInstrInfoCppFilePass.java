package vadl.lcb.template.lib.Target;

import static vadl.error.Diagnostic.error;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;
import static vadl.viam.ViamError.unwrap;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediatePredicateFunctionProvider;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * This file contains the logic for adjusting registers in instructions.
 */
public class EmitInstrInfoCppFilePass extends LcbTemplateRenderingPass {

  public EmitInstrInfoCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/InstrInfo.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "InstrInfo.cpp";
  }

  /**
   * An {@link Instruction} for copying a register.
   *
   * @param instruction      is the machine instruction which does the copying.
   * @param srcRegisterFile  is the register file for the source register in LLVM.
   * @param destRegisterFile is the register file for the destination register in LLVM.
   */
  record CopyPhysRegInstruction(Instruction instruction, RegisterFile srcRegisterFile,
                                RegisterFile destRegisterFile) {
  }

  /**
   * An {@link Instruction} for storing on the stack.
   *
   * @param instruction      is the machine instruction which does the storing.
   * @param destRegisterFile is the register file for the destination register in LLVM.
   * @param words            indicates how many words are stored.
   */
  record StoreRegSlot(Instruction instruction, RegisterFile destRegisterFile, int words) {

  }

  /**
   * An {@link Instruction} for loading from the stack.
   *
   * @param instruction      is the machine instruction which does the loading.
   * @param destRegisterFile is the register file for the destination register in LLVM.
   * @param words            indicates how many words are stored.
   */
  record LoadRegSlot(Instruction instruction, RegisterFile destRegisterFile, int words) {

  }

  /**
   * An entry to adapt a register with an immediate.
   */
  record AdjustRegCase(Instruction instruction,
                       Identifier predicate,
                       RegisterFile destRegisterFile,
                       RegisterFile srcRegisterFile) {

  }

  private List<CopyPhysRegInstruction> getMovInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var addi32 = mapWithInstructionLabel(MachineInstructionLabel.ADDI_32, isaMatching);
    var addi64 = mapWithInstructionLabel(MachineInstructionLabel.ADDI_64, isaMatching);

    return Stream.concat(addi32.stream(), addi64.stream()).toList();
  }

  private List<StoreRegSlot> getStoreMemoryInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions =
        (List<Instruction>) isaMatching.getOrDefault(MachineInstructionLabel.STORE_MEM,
            Collections.emptyList());

    var mapped = instructions.stream()
        .map(i -> {
          var destRegisterFile =
              ensurePresent(i.behavior().getNodes(ReadRegFileNode.class).findFirst(),
                  "There must be destination register").registerFile();
          var words =
              ensurePresent(i.behavior().getNodes(WriteMemNode.class).findFirst(),
                  "There must be a write mem node").words();
          return new StoreRegSlot(i, destRegisterFile, words);
        })
        // Sort by largest word size descending
        .sorted((storeRegSlot, t1) -> Integer.compare(t1.words, storeRegSlot.words))
        .toList();

    return mapped;
  }

  private List<LoadRegSlot> getLoadMemoryInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions =
        (List<Instruction>) isaMatching.getOrDefault(MachineInstructionLabel.LOAD_MEM,
            Collections.emptyList());

    return instructions.stream()
        .map(i -> {
          var destRegisterFile =
              ensurePresent(i.behavior().getNodes(WriteRegFileNode.class).findFirst(),
                  "There must be destination register").registerFile();
          var words =
              ensurePresent(i.behavior().getNodes(ReadMemNode.class).findFirst(),
                  "There must be a read mem node").words();
          return new LoadRegSlot(i, destRegisterFile, words);
        })
        // Sort by largest word size descending
        .sorted((loadRegSlot, t1) -> Integer.compare(t1.words, loadRegSlot.words))
        .toList();
  }

  private List<CopyPhysRegInstruction> mapWithInstructionLabel(
      MachineInstructionLabel label,
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions = (List<Instruction>)
        isaMatching.getOrDefault(label, Collections.emptyList());

    return instructions.stream()
        .map(i -> {
          var destRegisterFile =
              ensurePresent(i.behavior().getNodes(WriteRegFileNode.class).findFirst(),
                  "There must be destination register").registerFile();
          var srcRegisterFile =
              ensurePresent(i.behavior().getNodes(ReadRegFileNode.class).findFirst(),
                  "There must be source register").registerFile();

          return new CopyPhysRegInstruction(i, srcRegisterFile, destRegisterFile);
        })
        .toList();
  }

  private Instruction getAddition(Map<MachineInstructionLabel, List<Instruction>> isaMatches) {
    var add64 = isaMatches.get(MachineInstructionLabel.ADDI_64);

    if (add64 == null) {
      var instructions = isaMatches.get(MachineInstructionLabel.ADDI_32);
      ensureNonNull(instructions, "instructions with addition and immediate exist");
      return ensurePresent(instructions.stream().findFirst(),
          "There must be at least one instruction");
    } else {
      return ensurePresent(add64.stream().findFirst(), "There must be at least one instruction");
    }
  }

  private int getImmBitSize(IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
                            Instruction addition) {
    var fields = fieldUsages.getImmediates(addition.format());
    verifyInstructionHasOnlyOneImm(addition, fields);
    return ensurePresent(fields.stream().findFirst(), "already checked that it is present").size();
  }

  private void verifyInstructionHasOnlyOneImm(Instruction addition, List<Format.Field> fields) {
    ensure(fields.size() == 1, () -> error(
        "The compiler requires an addition with immediate with only one immediate. "
            + "The detected instruction has zero or more than one.",
        addition.sourceLocation())
    );
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var isaMatches = (Map<MachineInstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMachineInstructionMatchingPass.class);
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);
    var addition = getAddition(isaMatches);
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "copyPhysInstructions", getMovInstructions(isaMatches),
        "storeStackSlotInstructions", getStoreMemoryInstructions(isaMatches),
        "loadStackSlotInstructions", getLoadMemoryInstructions(isaMatches),
        "additionImmInstruction", addition,
        "additionImmSize", getImmBitSize(fieldUsages, addition)
    );
  }
}
