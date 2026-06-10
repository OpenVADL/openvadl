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

package vadl.lcb.template.lib.Target;

import static vadl.lcb.template.utils.AbiSequencesUtil.createConstantSequences;
import static vadl.lcb.template.utils.AbiSequencesUtil.createRegisterAdjustment;
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;
import static vadl.viam.ViamError.unwrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.MachineInstructionLabelGroup;
import vadl.gcb.passes.PseudoInstructionLabel;
import vadl.gcb.valuetypes.CompilerRegisterUtils;
import vadl.lcb.passes.TableGenInstructionCtx;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediatePredicateFunctionProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.types.BuiltInTable;
import vadl.viam.ArtificialResource;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.HasRegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteMemNode;

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
    var processorName = lcbConfiguration().targetName().value();
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
  record CopyPhysRegInstruction(Instruction instruction,
                                List<RegisterResource> srcRegisterFile,
                                List<RegisterResource> destRegisterFile) {
  }

  /**
   * An {@link Instruction} for copying a register.
   *
   * @param instruction      is the machine instruction which does the copying.
   * @param destRegisterFile is the register file for the destination register in LLVM.
   * @param zeroRegister     is the name of the zero register in the register file.
   */
  record TruncateInstruction(Instruction instruction,
                             RegisterResource destRegisterFile,
                             String zeroRegister) implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "instruction", instruction.identifier.simpleName(),
          "destRegisterFile", destRegisterFile.identifier().simpleName(),
          "zeroRegister", zeroRegister
      );
    }
  }

  /**
   * An {@link Instruction} for storing on the stack.
   *
   * @param instruction     is the machine instruction which does the storing.
   * @param srcRegisterFile is the register file from which the value was read.
   * @param words           indicates how many words are stored.
   */
  record StoreRegSlot(Instruction instruction, RegisterResource srcRegisterFile, int words) {

  }

  /**
   * An {@link Instruction} for loading from the stack.
   *
   * @param instruction      is the machine instruction which does the loading.
   * @param destRegisterFile is the register file for the destination register in LLVM.
   * @param words            indicates how many words are stored.
   */
  record LoadRegSlot(Instruction instruction, RegisterResource destRegisterFile, int words) {

  }

  private List<CopyPhysRegInstruction> physInstructions(
      Specification viam,
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var addi32 = mapWithInstructionLabel(viam, MachineInstructionLabel.ADDI_32, isaMatching);
    var addi64 = mapWithInstructionLabel(viam, MachineInstructionLabel.ADDI_64, isaMatching);

    return Stream.concat(addi32.stream(), addi64.stream()).toList();
  }

  private List<TruncateInstruction> truncateInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    return mapTruncateInstructionsWithInstructionLabel(
        MachineInstructionLabel.OR,
        isaMatching);
  }

  private List<StoreRegSlot> getStoreMemoryInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions =
        isaMatching.getOrDefault(MachineInstructionLabel.STORE_MEM_WITH_IMMEDIATE,
            Collections.emptyList());

    return instructions.stream()
        .flatMap(i -> {
          var writeMemNode = ensurePresent(i.behavior().getNodes(WriteMemNode.class).findFirst(),
              "There must be a write mem node");
          var valueNodes = new ArrayList<Node>();
          valueNodes.add(writeMemNode.value());
          writeMemNode.value().collectInputsWithChildren(valueNodes);
          var srcRegisterFile =
              ensurePresent(valueNodes.stream().filter(x -> x instanceof ReadsRegisterTensor)
                      .map(x -> ((ReadsRegisterTensor) x).registerResource())
                      .findFirst(),
                  () -> Diagnostic.error("There must be register or alias as source.",
                      writeMemNode.location()));
          var words = writeMemNode.words();

          // If the registerFile is an alias, we also need to store it for the original reference.
          // However, we skip it when it is an alias with a smaller type.
          var base = new StoreRegSlot(i, srcRegisterFile, words);
          if (srcRegisterFile instanceof ArtificialResource artificialResource
              && artificialResource.innerResourceRef() instanceof RegisterTensor registerTensor
              && artificialResource.type().equals(registerTensor.type())) {
            return Stream.of(base,
                new StoreRegSlot(i, registerTensor, words));
          } else {
            return Stream.of(base);
          }
        })
        // Sort by largest word size descending
        .sorted((storeRegSlot, t1) -> Integer.compare(t1.words, storeRegSlot.words))
        .toList();
  }

  private List<LoadRegSlot> getLoadMemoryInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions =
        isaMatching.getOrDefault(MachineInstructionLabel.LOAD_MEM_WITH_IMMEDIATE,
            Collections.emptyList());

    return instructions.stream()
        .flatMap(i -> {
          var destRegisterFile =
              ensurePresent(i.behavior().getNodes(WritesRegisterTensor.class)
                      .filter(HasRegisterTensor::hasRegisterFile)
                      .findFirst(),
                  () -> Diagnostic.error("There must be a destination register file",
                      i.location())).registerResource();
          var words =
              ensurePresent(i.behavior().getNodes(ReadMemNode.class).findFirst(),
                  "There must be a read mem node").words();

          // If the registerFile is an alias, we also need to load it for the original reference.
          // However, we skip it when it is an alias with a smaller type.
          var base = new LoadRegSlot(i, destRegisterFile, words);
          if (destRegisterFile instanceof ArtificialResource artificialResource
              && artificialResource.innerResourceRef() instanceof RegisterTensor registerTensor
              && artificialResource.type().equals(registerTensor.type())) {
            return Stream.of(base,
                new LoadRegSlot(i, registerTensor, words));
          } else {
            return Stream.of(base);
          }
        })
        // Sort by largest word size descending
        .sorted((loadRegSlot, t1) -> Integer.compare(t1.words, loadRegSlot.words))
        .toList();
  }

  private List<TruncateInstruction> mapTruncateInstructionsWithInstructionLabel(
      MachineInstructionLabel label,
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions =
        isaMatching.getOrDefault(label, Collections.emptyList());

    return instructions.stream()
        .flatMap(i -> {
          var destRegisterFile =
              ensurePresent(i.behavior().getNodes(WritesRegisterTensor.class)
                      .filter(HasRegisterTensor::hasRegisterFile)
                      .findFirst(),
                  "There must be destination register").registerResource();

          var zeroRegister = ensurePresent(CompilerRegisterUtils.zeroRegister(destRegisterFile),
              () -> Diagnostic.error("There is no zero register for the register file",
                  destRegisterFile.location()))
              .stream()
              .findFirst();

          var zeroRegisterValue = ensurePresent(zeroRegister,
              () -> Diagnostic.error("List has no zero registers", destRegisterFile.location()));

          // If the registerFile is an alias, we also need to truncate it for the original
          // reference.
          // However, we skip it when it is an alias with a smaller type.
          var base = new TruncateInstruction(i, destRegisterFile,
              CompilerRegisterUtils.indexedRegisterName(destRegisterFile,
                  zeroRegisterValue.intValue()));
          if (destRegisterFile instanceof ArtificialResource artificialResource
              && artificialResource.innerResourceRef() instanceof RegisterTensor registerTensor
              && artificialResource.type().equals(registerTensor.type())) {
            return Stream.of(base,
                new TruncateInstruction(i, registerTensor,
                    CompilerRegisterUtils.indexedRegisterName(registerTensor,
                        zeroRegisterValue.intValue())));
          } else {
            return Stream.of(base);
          }
        })
        .toList();
  }

  private List<CopyPhysRegInstruction> mapWithInstructionLabel(
      Specification viam,
      MachineInstructionLabel label,
      Map<MachineInstructionLabel, List<Instruction>> isaMatching) {
    var instructions =
        isaMatching.getOrDefault(label, Collections.emptyList());

    return instructions.stream()
        .map(i -> {
          var destRegisterFile =
              ensurePresent(i.behavior().getNodes(WritesRegisterTensor.class)
                      .filter(HasRegisterTensor::hasRegisterFile)
                      .findFirst(),
                  "There must be destination register").registerTensor();

          var destAliases = viam.isa().get().artificialResources()
              .stream()
              .filter(x -> x.aliasSlice() == null)
              .filter(ArtificialResource::isRegisterFile)
              .filter(x -> x.innerResourceRef() == destRegisterFile)
              .toList();

          var srcRegisterFile =
              ensurePresent(i.behavior().getNodes(ReadsRegisterTensor.class)
                      .filter(HasRegisterTensor::hasRegisterFile)
                      .findFirst(),
                  "There must be source register").registerTensor();

          var srcAliases = viam.isa().get().artificialResources()
              .stream()
              .filter(x -> x.aliasSlice() == null)
              .filter(ArtificialResource::isRegisterFile)
              .filter(x -> x.innerResourceRef() == destRegisterFile)
              .toList();

          List<RegisterResource> srcResult = new ArrayList<>(srcAliases);
          srcResult.add(srcRegisterFile);

          List<RegisterResource> destResult = new ArrayList<>(destAliases);
          destResult.add(destRegisterFile);

          return new CopyPhysRegInstruction(i, srcResult, destResult);
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
      ensureNonNull(instructions, "instructions with addition have to exist");
      return ensurePresent(instructions.stream().findFirst(),
          "There must be at least one instruction");
    } else {
      return ensurePresent(add64.stream().findFirst(), "There must be at least one instruction");
    }
  }

  /**
   * Return the name of the unconditional jump instruction.
   */
  private String getJump(Specification specification,
                         Map<MachineInstructionLabel,
                             List<Instruction>> instructionMatches,
                         Map<PseudoInstructionLabel,
                             List<PseudoInstruction>> pseudoMatches) {
    return getJumpFromMachineInstructions(instructionMatches)
        .or(() -> getJumpFromPseudoInstructions(pseudoMatches))
        .orElseThrow(() -> Diagnostic.error(
            "The compiler generator requires an instruction / a pseudo instruction which is "
                + "an unconditional jump. We haven't found one.",
            specification.location()).build());
  }

  private Optional<String> getJumpFromPseudoInstructions(
      Map<PseudoInstructionLabel,
          List<PseudoInstruction>> pseudoMatches) {
    return Optional.ofNullable(pseudoMatches.get(PseudoInstructionLabel.J))
        .map(x -> x.stream().findFirst().get())
        .map(Definition::simpleName);
  }

  private Optional<String> getJumpFromMachineInstructions(
      Map<MachineInstructionLabel,
          List<Instruction>> machineMatches) {
    return Optional.ofNullable(machineMatches.get(MachineInstructionLabel.J))
        .map(x -> x.stream().findFirst().get())
        .map(Definition::simpleName);
  }

  record BranchInstruction(String name,
                           int bitWidth, /* size of the immediate */
                           String predicateMethod) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "bitWidth", bitWidth,
          "predicateMethod", predicateMethod
      );
    }
  }

  record PseudoBranchInstruction(String name,
                                 int bitWidth /* size of the immediate */) implements
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

  private RegisterTensor getRegisterClassFromInstruction(Instruction instruction) {
    return
        ensurePresent(
            instruction.behavior()
                .getNodes(ReadsRegisterTensor.class)
                .filter(HasRegisterTensor::hasRegisterFile)
                .map(HasRegisterTensor::registerTensor)
                .findFirst(), () -> Diagnostic.error(
                "Expected that the instruction has at least one register file",
                instruction.location()));
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
    var additionRR = getAdditionRR(isaMatches);
    var additionRegisterFile = getRegisterClassFromInstruction(additionRR);
    var jumpInstructionName = getJump(specification, isaMatches, pseudoMatches);

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, lcbConfiguration().targetName().value().toLowerCase());
    map.put("copyPhysInstructions",
        physInstructions(specification, isaMatches).stream().map(this::map).toList());
    map.put("truncateInstructions",
        truncateInstructions(isaMatches)
    );
    map.put("storeStackSlotInstructions",
        getStoreMemoryInstructions(isaMatches).stream().map(this::map).toList());
    map.put("loadStackSlotInstructions",
        getLoadMemoryInstructions(isaMatches).stream().map(this::map).toList());
    map.put("additionImm", additionRI.simpleName());
    map.put("addition", additionRR.simpleName());
    map.put("additionRegisterFile", additionRegisterFile.simpleName());
    map.put("machineBranchInstructions", machineBranchInstructions(specification, passResults));
    map.put("pseudoBranchInstructions",
        pseudoBranchInstructions(specification, passResults, fieldUsages));
    map.put("instructionSizes", instructionSizes(specification));
    map.put("jumpInstruction", jumpInstructionName);
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
    map.put("constantSequences", createConstantSequences(specification));
    map.put("registerAdjustmentSequences", createRegisterAdjustment(specification));

    return map;
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

  private List<BranchInstruction> machineBranchInstructions(
      Specification specification,
      PassResults passResults) {
    var branchInstructions = new ArrayList<BranchInstruction>();
    var database = new Database(passResults, specification);

    machineInstructions(passResults, database, branchInstructions);

    return branchInstructions
        .stream()
        .sorted(Comparator.comparing(BranchInstruction::name))
        .collect(Collectors.toList());
  }

  private List<PseudoBranchInstruction> pseudoBranchInstructions(
      Specification specification,
      PassResults passResults,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages) {
    var branchInstructions = new ArrayList<PseudoBranchInstruction>();
    var database = new Database(passResults, specification);

    pseudoInstructions(fieldUsages, database, branchInstructions);

    return branchInstructions.stream()
        .sorted(Comparator.comparing(PseudoBranchInstruction::name))
        .collect(Collectors.toList());
  }

  private static void pseudoInstructions(
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Database database,
      List<PseudoBranchInstruction> branchInstructions) {
    var result = database.run(
        new Query.Builder().pseudoInstructionLabel(PseudoInstructionLabel.J).build());

    for (var pseudoInstruction : result.pseudoInstructions()) {
      var callNodes = pseudoInstruction.behavior().getNodes(InstrCallNode.class).toList();

      for (var callNode : callNodes) {
        var machineInstruction = callNode.target();
        var immediates = fieldUsages.getImmediateFields(machineInstruction);
        ensure(immediates.size() == 1,
            () -> Diagnostic.error("We only support branch instructions with one label.",
                machineInstruction.location()));
        var immediate = unwrap(immediates.stream().findFirst());
        int bitWidth = immediate.size();
        branchInstructions.add(
            new PseudoBranchInstruction(pseudoInstruction.identifier.simpleName(), bitWidth));
      }
    }
  }

  private static void machineInstructions(
      PassResults passResults,
      Database database,
      List<BranchInstruction> branchInstructions) {
    var lookup =
        ImmediatePredicateFunctionProvider.predicateFunctionsByFieldAccess(passResults);
    var result = database.run(new Query.Builder().machineInstructionLabels(List.of(
        MachineInstructionLabel.J,
        MachineInstructionLabel.BEQ,
        MachineInstructionLabel.BNEQ,
        MachineInstructionLabel.BSGEQ,
        MachineInstructionLabel.BSGTH,
        MachineInstructionLabel.BSLEQ,
        MachineInstructionLabel.BSLTH,
        MachineInstructionLabel.BUGEQ,
        MachineInstructionLabel.BUGTH,
        MachineInstructionLabel.BULEQ,
        MachineInstructionLabel.BULTH,
        MachineInstructionLabel.BEQ_BY_STATUS_REGISTER,
        MachineInstructionLabel.BNEQ_BY_STATUS_REGISTER,
        MachineInstructionLabel.BSGEQ_BY_STATUS_REGISTER,
        MachineInstructionLabel.BSLEQ_BY_STATUS_REGISTER,
        MachineInstructionLabel.BSLTH_BY_STATUS_REGISTER,
        MachineInstructionLabel.BSGTH_BY_STATUS_REGISTER
    )).build());

    final Predicate<Node> isPc = (x) -> x instanceof ReadRegTensorNode node && node.isPcAccess();
    for (var machineInstruction : result.machineInstructions()) {
      var builtins = machineInstruction
          .behavior()
          .getNodes(BuiltInCall.class)
          .filter(x -> x.builtIn() == BuiltInTable.ADD && x.arguments().size() == 2)
          .filter(x -> isPc.test(x.arguments().getFirst()) || isPc.test(x.arguments().get(1)))
          .toList();
      var immediates = new ArrayList<FieldAccessRefNode>();
      builtins.forEach(builtInCall -> builtInCall.collectInputsWithChildren(immediates,
          FieldAccessRefNode.class));
      ensure(immediates.size() == 1,
          () -> Diagnostic.error("We only support branch instructions with one label.",
              machineInstruction.location()));
      var immediate = unwrap(immediates.stream().findFirst());
      var function = ensureNonNull(lookup.get(immediate.fieldAccess()),
          () -> Diagnostic.error("Cannot find field access' predicate", immediate.location()));
      var functionName = function.header().functionName().lower();
      int bitWidth = immediate.fieldAccess().type().asDataType().bitWidth();
      branchInstructions.add(
          new BranchInstruction(machineInstruction.identifier.simpleName(),
              bitWidth,
              functionName));
    }
  }

  private Map<String, Object> map(CopyPhysRegInstruction obj) {
    return Map.of(
        "destRegisterFile",
        obj.destRegisterFile.stream().map(x -> x.identifier().simpleName()).toList(),
        "srcRegisterFile",
        obj.srcRegisterFile.stream().map(x -> x.identifier().simpleName()).toList(),
        "instruction", obj.instruction.simpleName()
    );
  }

  private Map<String, Object> map(StoreRegSlot obj) {
    return Map.of(
        "srcRegisterFile", obj.srcRegisterFile.simpleName(),
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
      var pair =
          ensurePresent(
              fieldUsages.fieldsByRegisterUsage(instruction,
                      IdentifyFieldUsagePass.RegisterUsage.SOURCE)
                  .stream().findFirst(),
              () -> Diagnostic.error("Cannot find a register operand.",
                  instruction.location()));
      var field = pair.left();
      var registerOrRegisterFile = pair.right();

      var immediate =
          ensurePresent(fieldUsages.getImmediateFields(instruction).stream().findFirst(),
              () -> Diagnostic.error("Cannot find an immediate operand.",
                  instruction.location()));

      var ctx = instruction.extension(TableGenInstructionCtx.class);
      var loweringRecord =
          ensureNonNull(ctx,
              () -> Diagnostic.error("Cannot find a TableGen record for this instruction.",
                  instruction.location())).record();

      // MCInst have the output at the beginning.
      // Therefore, we need to offset the inputs.
      var regIndex =
          loweringRecord.info().outputs().size() + loweringRecord.info().findInputIndex(field);
      var immIndex =
          loweringRecord.info().outputs().size() + loweringRecord.info().findInputIndex(immediate);

      var isCheckable = false;
      var zeroRegister = "";

      // Is it a register file?
      if (registerOrRegisterFile.registerFile() != null) {
        var registerFile = registerOrRegisterFile.registerFile();
        var zeroRegisterAddr = CompilerRegisterUtils.zeroRegister(registerFile);
        if (zeroRegisterAddr.isPresent()) {
          zeroRegister =
              CompilerRegisterUtils.indexedRegisterName(registerFile,
                  zeroRegisterAddr.get().getFirst().intValue());
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

    return aggregates.stream().sorted(Comparator.comparing(o -> o.instructionName)).toList();
  }
}
