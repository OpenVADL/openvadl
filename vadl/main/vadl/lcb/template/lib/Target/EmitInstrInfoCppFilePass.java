package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediatePredicateFunctionProvider;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
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
                       RegisterFile destRegisterFile) {

  }

  private List<CopyPhysRegInstruction> getMovInstructions(
      HashMap<InstructionLabel, List<Instruction>> isaMatching) {
    var addi32 = mapWithInstructionLabel(InstructionLabel.ADDI_32, isaMatching);
    var addi64 = mapWithInstructionLabel(InstructionLabel.ADDI_64, isaMatching);

    return Stream.concat(addi32.stream(), addi64.stream()).toList();
  }

  private List<StoreRegSlot> getStoreMemoryInstructions(
      HashMap<InstructionLabel, List<Instruction>> isaMatching) {
    var instructions = (List<Instruction>) isaMatching.getOrDefault(InstructionLabel.STORE_MEM,
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
      HashMap<InstructionLabel, List<Instruction>> isaMatching) {
    var instructions = (List<Instruction>) isaMatching.getOrDefault(InstructionLabel.LOAD_MEM,
        Collections.emptyList());

    var mapped = instructions.stream()
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

    return mapped;
  }

  private List<CopyPhysRegInstruction> mapWithInstructionLabel(
      InstructionLabel label,
      HashMap<InstructionLabel, List<Instruction>> isaMatching) {
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

  private Instruction getAddition(HashMap<InstructionLabel, List<Instruction>> isaMatches) {
    var add64 = isaMatches.get(InstructionLabel.ADDI_64);

    if (add64 == null) {
      var instructions = isaMatches.get(InstructionLabel.ADDI_32);
      ensureNonNull(instructions, "instructions with addition and immediate exist");
      return ensurePresent(instructions.stream().findFirst(),
          "There must be at least one instruction");
    } else {
      return ensurePresent(add64.stream().findFirst(), "There must be at least one instruction");
    }
  }

  private int getImmBitSize(DetectImmediatePass.ImmediateDetectionContainer fieldUsages,
                            Instruction addition) {
    var fields = fieldUsages.getImmediates(addition.format());
    verifyInstructionHasOnlyOneImm(addition, fields);
    return ensurePresent(fields.stream().findFirst(), "already checked that it is present").size();
  }

  private void verifyInstructionHasOnlyOneImm(Instruction addition, List<Format.Field> fields) {
    ensure(fields.size() == 1, () -> Diagnostic.error(
            "The compiler requires an addition with immediate with only one immediate. "
                + "The detected instruction has zero or more than one.",
            addition.sourceLocation())
        .build());
  }

  private List<AdjustRegCase> getAdjustCases(PassResults passResults,
                                             HashMap<InstructionLabel, List<Instruction>>
                                                 isaMatches,
                                             DetectImmediatePass.ImmediateDetectionContainer
                                                 fieldUsages) {
    var predicates = ImmediatePredicateFunctionProvider.generatePredicateFunctions(passResults);
    var addi32 = isaMatches.getOrDefault(InstructionLabel.ADDI_32, Collections.emptyList());
    var addi64 = isaMatches.getOrDefault(InstructionLabel.ADDI_64, Collections.emptyList());

    return Stream.concat(addi32.stream(), addi64.stream())
        .map(addImm -> {
          var fields = fieldUsages.getImmediates(addImm.format());
          verifyInstructionHasOnlyOneImm(addImm, fields);
          var imm =
              ensurePresent(fields.stream().findFirst(), "already checked that it is present");
          var destRegisterFile =
              ensurePresent(addImm.behavior().getNodes(WriteRegFileNode.class).findFirst(),
                  "There must be destination register").registerFile();
          return new AdjustRegCase(addImm,
              ensureNonNull(predicates.get(imm), "predicate must exist").identifier,
              destRegisterFile);
        })
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var isaMatches = (HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMatchingPass.class);
    var fieldUsages = (DetectImmediatePass.ImmediateDetectionContainer) passResults.lastResultOf(
        DetectImmediatePass.class);
    var addition = getAddition(isaMatches);
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "copyPhysInstructions", getMovInstructions(isaMatches),
        "storeStackSlotInstructions", getStoreMemoryInstructions(isaMatches),
        "loadStackSlotInstructions", getLoadMemoryInstructions(isaMatches),
        "additionImmInstruction", addition,
        "additionImmSize", getImmBitSize(fieldUsages, addition),
        "adjustCases", getAdjustCases(passResults, isaMatches, fieldUsages)
    );
  }
}
