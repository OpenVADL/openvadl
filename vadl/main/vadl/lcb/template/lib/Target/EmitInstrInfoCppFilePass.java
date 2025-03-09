// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.template.lib.Target;

import static vadl.error.Diagnostic.error;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;
import static vadl.viam.ViamError.unwrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.MachineInstructionLabelGroup;
import vadl.gcb.passes.PseudoInstructionLabel;
import vadl.gcb.passes.ValueRange;
import vadl.gcb.passes.ValueRangeCtx;
import vadl.lcb.passes.TableGenInstructionCtx;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;
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

  private Instruction getAdditionRI(Map<MachineInstructionLabel, List<Instruction>> isaMatches) {
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

  private Instruction getAdditionRR(Map<MachineInstructionLabel, List<Instruction>> isaMatches) {
    var add64 = isaMatches.get(MachineInstructionLabel.ADD_64);

    if (add64 == null) {
      var instructions = isaMatches.get(MachineInstructionLabel.ADD_32);
      ensureNonNull(instructions, "instructions with addition exist");
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
                            Instruction additionRI) {
    var fields = fieldUsages.getImmediates(additionRI);
    verifyInstructionHasOnlyOneImm(additionRI, fields);
    return ensurePresent(fields.stream().findFirst(), "already checked that it is present").size();
  }

  private void verifyInstructionHasOnlyOneImm(Instruction addition, List<Format.Field> fields) {
    ensure(fields.size() == 1, () -> error(
        "The compiler requires an addition with immediate with only one immediate. "
            + "The detected instruction has zero or more than one.",
        addition.sourceLocation())
    );
  }

  record BranchInstruction(String name, /* size of the immediate */ int bitWidth) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "bitWidth", bitWidth
      );
    }
  }

  record InstructionSize(String name, /* format size */ int byteSize) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "byteSize", byteSize
      );
    }
  }

  /**
   * There are several instructions which are marked as `isAsCheapMoveAggregate`. However, it
   * sometimes depends. Particularly, {@code ADDI} etc. must have as a register argument the
   * zero register or an immediate which is zero.
   */
  record IsAsCheapMoveAggregate(String instructionName,
                                int regOperand,
                                int immOperand,
                                String zeroRegister,
                                // when there is no zeroRegister then some
                                // instructions are never as cheap as move.
                                boolean isCheckable) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "instructionName", instructionName,
          "regOperand", regOperand,
          "immOperand", immOperand,
          "zeroRegister", zeroRegister,
          "isCheckable", isCheckable
      );
    }
  }

  private RegisterFile getRegisterClassFromInstruction(Instruction instruction) {
    return
        ensurePresent(
            instruction.behavior()
                .getNodes(ReadRegFileNode.class)
                .map(ReadRegFileNode::registerFile)
                .findFirst(), () -> Diagnostic.error(
                "Expected that the instruction has at least one register file",
                instruction.sourceLocation()));
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var isaMatches = ((IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
        IsaMachineInstructionMatchingPass.class)).labels();
    var pseudoMatches =
        ((IsaPseudoInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class)).labels();
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);
    var additionRI = getAdditionRI(isaMatches);
    var additionRIValueRange = valueRange(additionRI);
    var additionRR = getAdditionRR(isaMatches);
    var additionRegisterFile = getRegisterClassFromInstruction(additionRR);
    // Integer of the index of the zero register in the register file.
    var zeroRegisterIndex =
        ensurePresent(
            Arrays.stream(additionRegisterFile.constraints()).filter(x -> x.value().intValue() == 0)
                .map(
                    RegisterFile.Constraint::address)
                .map(Constant.Value::intValue)
                .findFirst(),
            () -> Diagnostic.error("Cannot find a zero register for the register file",
                additionRegisterFile.sourceLocation()));
    var jump = getJump(specification, pseudoMatches);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, lcbConfiguration().processorName().value().toLowerCase());
    map.put("copyPhysInstructions",
        getMovInstructions(isaMatches).stream().map(this::map).toList());
    map.put("storeStackSlotInstructions",
        getStoreMemoryInstructions(isaMatches).stream().map(this::map).toList());
    map.put("loadStackSlotInstructions",
        getLoadMemoryInstructions(isaMatches).stream().map(this::map).toList());
    map.put("additionImm", additionRI.simpleName());
    map.put("additionImmHighestValue", additionRIValueRange.highest());
    map.put("additionImmLowestValue", additionRIValueRange.lowest());
    map.put("addition", additionRR.simpleName());
    map.put("additionRegisterFile", additionRegisterFile.simpleName());
    map.put("additionImmSize", getImmBitSize(fieldUsages, additionRI));
    map.put("zeroRegisterIndex", zeroRegisterIndex);
    map.put("branchInstructions", getBranchInstructions(specification, passResults, fieldUsages));
    map.put("instructionSizes", instructionSizes(specification));
    map.put("jumpInstruction", jump.simpleName());
    map.put("beq",
        getBranchInstruction(specification, passResults, MachineInstructionLabel.BEQ));
    map.put("bne", getBranchInstruction(specification, passResults,
        MachineInstructionLabel.BNEQ));
    map.put("blt", getBranchInstruction(specification, passResults,
        MachineInstructionLabel.BSLTH));
    map.put("bge", getBranchInstruction(specification, passResults,
        MachineInstructionLabel.BSGEQ));
    map.put("bltu",
        getBranchInstruction(specification, passResults,
            MachineInstructionLabel.BULTH));
    map.put("bgeu",
        getBranchInstruction(specification, passResults,
            MachineInstructionLabel.BUGEQ));
    map.put("isAsCheapAsMove",
        areAsCheapAsMove(fieldUsages, new Database(passResults, specification)));

    return map;
  }

  private ValueRange valueRange(Instruction instruction) {
    var ctx = ensureNonNull(instruction.extension(ValueRangeCtx.class),
        () -> Diagnostic.error("Has no extension value range", instruction.sourceLocation()));
    return ensurePresent(ctx.getFirst(),
        () -> Diagnostic.error("Has no value range", instruction.sourceLocation()));
  }

  @Nullable
  private String getBranchInstruction(Specification specification,
                                      PassResults passResults,
                                      MachineInstructionLabel machineInstructionLabel) {
    var database = new Database(passResults, specification);
    var result = database.run(new Query.Builder().machineInstructionLabels(List.of(
        machineInstructionLabel
    )).build());
    return result.machineInstructions().stream().findFirst().map(Definition::simpleName)
        .orElse(null);
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

    machineInstructions(fieldUsages, database, branchInstructions);
    pseudoInstructions(fieldUsages, database, branchInstructions);

    return branchInstructions;
  }

  private static void pseudoInstructions(
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Database database,
      List<BranchInstruction> branchInstructions) {
    var result = database.run(
        new Query.Builder().pseudoInstructionLabel(PseudoInstructionLabel.J).build());

    for (var pseudoInstruction : result.pseudoInstructions()) {
      var callNodes = pseudoInstruction.behavior().getNodes(InstrCallNode.class).toList();

      for (var callNode : callNodes) {
        var machineInstruction = callNode.target();
        var immediates = fieldUsages.getImmediates(machineInstruction);
        ensure(immediates.size() == 1,
            () -> Diagnostic.error("We only support branch instructions with one label.",
                machineInstruction.sourceLocation()));
        var immediate = unwrap(immediates.stream().findFirst());
        int bitWidth = immediate.size();
        branchInstructions.add(
            new BranchInstruction(pseudoInstruction.identifier.simpleName(), bitWidth));
      }
    }
  }

  private static void machineInstructions(
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Database database,
      List<BranchInstruction> branchInstructions) {
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
      var immediates = fieldUsages.getImmediates(machineInstruction);
      ensure(immediates.size() == 1,
          () -> Diagnostic.error("We only support branch instructions with one label.",
              machineInstruction.sourceLocation()));
      var immediate = unwrap(immediates.stream().findFirst());
      int bitWidth = immediate.size();
      branchInstructions.add(
          new BranchInstruction(machineInstruction.identifier.simpleName(), bitWidth));
    }
  }

  private Map<String, Object> map(CopyPhysRegInstruction obj) {
    return Map.of(
        "destRegisterFile", obj.destRegisterFile.simpleName(),
        "srcRegisterFile", obj.srcRegisterFile.simpleName(),
        "instruction", obj.instruction.simpleName()
    );
  }

  private Map<String, Object> map(StoreRegSlot obj) {
    return Map.of(
        "destRegisterFile", obj.destRegisterFile.simpleName(),
        "instruction", obj.instruction.simpleName()
    );
  }

  private Map<String, Object> map(LoadRegSlot obj) {
    return Map.of(
        "destRegisterFile", obj.destRegisterFile.simpleName(),
        "instruction", obj.instruction.simpleName()
    );
  }

  private List<IsAsCheapMoveAggregate> areAsCheapAsMove(
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Database database) {
    var aggregates = new ArrayList<IsAsCheapMoveAggregate>();
    var instructions = database.run(new Query.Builder().machineInstructionLabelGroup(
            MachineInstructionLabelGroup.AS_CHEAP_AS_MOVE_CANDIDATES).build())
        .machineInstructions().stream().toList();

    for (var instruction : instructions) {
      var reg =
          ensurePresent(
              fieldUsages.fieldsByRegisterUsage(instruction,
                      IdentifyFieldUsagePass.RegisterUsage.SOURCE)
                  .stream().findFirst(),
              () -> Diagnostic.error("Cannot find a register operand.",
                  instruction.sourceLocation()));

      var immediate =
          ensurePresent(fieldUsages.getImmediates(instruction).stream().findFirst(),
              () -> Diagnostic.error("Cannot find an immediate operand.",
                  instruction.sourceLocation()));

      var ctx = instruction.extension(TableGenInstructionCtx.class);
      var loweringRecord =
          ensureNonNull(ctx,
              () -> Diagnostic.error("Cannot find a TableGen record for this instruction.",
                  instruction.sourceLocation())).record();

      // MCInst have the output at the beginning.
      // Therefore, we need to offset the inputs.
      var regIndex =
          loweringRecord.info().outputs().size() + loweringRecord.info().findInputIndex(reg.left());
      var immIndex =
          loweringRecord.info().outputs().size() + loweringRecord.info().findInputIndex(immediate);

      var isCheckable = false;
      var zeroRegister = "";

      // Is it a register file?
      if (reg.right().isRight()) {
        var registerFile = reg.right().right();
        var zeroRegisterAddr = registerFile.zeroRegister();
        if (zeroRegisterAddr.isPresent()) {
          zeroRegister = registerFile.generateName(zeroRegisterAddr.get());
          isCheckable = true;
        }
      }

      aggregates.add(new IsAsCheapMoveAggregate(
          instruction.simpleName(),
          regIndex,
          immIndex,
          zeroRegister,
          isCheckable));
    }

    return aggregates;
  }
}
