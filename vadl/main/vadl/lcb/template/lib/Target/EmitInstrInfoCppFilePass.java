package vadl.lcb.template.lib.Target;

import static vadl.error.Diagnostic.error;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;
import static vadl.viam.ViamError.unwrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
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

  private PseudoInstruction getJump(Specification specification,
                                    Map<PseudoInstructionLabel,
                                        List<PseudoInstruction>> pseudoMatches) {
    var jump = Optional.ofNullable(pseudoMatches.get(PseudoInstructionLabel.J))
        .map(x -> x.stream().findFirst().get());
    return ensurePresent(jump,
        () -> Diagnostic.error(
            "Compiler generator requires a pseudo instruction for an unconditional jump",
            specification.sourceLocation()
        ));
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

  record BranchInstruction(String name, /* size of the immediate */ int bitWidth) {

  }

  record InstructionSize(String name, /* format size */ int byteSize) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var isaMatches = (Map<MachineInstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMachineInstructionMatchingPass.class);
    var pseudoMatches =
        (Map<PseudoInstructionLabel, List<PseudoInstruction>>) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class);
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);
    var addition = getAddition(isaMatches);
    var jump = getJump(specification, pseudoMatches);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, specification.simpleName());
    map.put("copyPhysInstructions", getMovInstructions(isaMatches));
    map.put("storeStackSlotInstructions", getStoreMemoryInstructions(isaMatches));
    map.put("loadStackSlotInstructions", getLoadMemoryInstructions(isaMatches));
    map.put("additionImmInstruction", addition);
    map.put("additionImmSize", getImmBitSize(fieldUsages, addition));
    map.put("branchInstructions", getBranchInstructions(specification, passResults, fieldUsages));
    map.put("instructionSizes", instructionSizes(specification));
    map.put("jumpInstruction", jump);
    map.put("beq", getBranchInstruction(specification, passResults, MachineInstructionLabel.BEQ));
    map.put("bne", getBranchInstruction(specification, passResults, MachineInstructionLabel.BNEQ));
    map.put("blt", getBranchInstruction(specification, passResults, MachineInstructionLabel.BSLTH));
    map.put("bge", getBranchInstruction(specification, passResults, MachineInstructionLabel.BSGEQ));
    map.put("bltu",
        getBranchInstruction(specification, passResults, MachineInstructionLabel.BULTH));
    map.put("bgeu",
        getBranchInstruction(specification, passResults, MachineInstructionLabel.BUGEQ));

    return map;
  }

  private Instruction getBranchInstruction(Specification specification,
                                           PassResults passResults,
                                           MachineInstructionLabel machineInstructionLabel) {
    var database = new Database(passResults, specification);
    var result = database.run(new Query.Builder().machineInstructionLabels(List.of(
        machineInstructionLabel
    )).build());
    return result.firstMachineInstruction();
  }

  private List<InstructionSize> instructionSizes(Specification specification) {
    return specification.isa().get().ownInstructions()
        .stream()
        .map(instruction -> new InstructionSize(instruction.identifier.simpleName(),
            instruction.format().type().bitWidth() / 8))
        .toList();
  }

  private List<BranchInstruction> getBranchInstructions(
      Specification specification,
      PassResults passResults,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages) {
    var branchInstructions = new ArrayList<BranchInstruction>();
    var database = new Database(passResults, specification);
    var result = database.run(new Query.Builder().machineInstructionLabels(List.of(
        MachineInstructionLabel.BEQ,
        MachineInstructionLabel.BNEQ,
        MachineInstructionLabel.BSGEQ,
        MachineInstructionLabel.BSGTH,
        MachineInstructionLabel.BSLEQ,
        MachineInstructionLabel.BSLTH,
        MachineInstructionLabel.BUGEQ,
        MachineInstructionLabel.BUGTH,
        MachineInstructionLabel.BULEQ,
        MachineInstructionLabel.BULTH
    )).build());

    for (var machineInstruction : result.machineInstructions()) {
      var immediates = fieldUsages.getImmediates(machineInstruction.format());
      ensure(immediates.size() == 1,
          () -> Diagnostic.error("We only support branch instructions with one label.",
              machineInstruction.sourceLocation()));
      var immediate = unwrap(immediates.stream().findFirst());
      int bitWidth = immediate.size();
      branchInstructions.add(
          new BranchInstruction(machineInstruction.identifier.simpleName(), bitWidth));
    }

    return branchInstructions;
  }
}
