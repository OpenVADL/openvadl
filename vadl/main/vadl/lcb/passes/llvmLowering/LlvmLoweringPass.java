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

package vadl.lcb.passes.llvmLowering;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.TableGenInstructionCtx;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringPseudoRecord;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringAddImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringDivisionAndRemainderStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringIndirectJumpStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringLoadUpperImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryLoadStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryStoreStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringXoriAndOriStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanImmediateUnsignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanSignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanUnsignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * This is a wrapper class which contains utility functions for the lowering.
 */
public class LlvmLoweringPass extends Pass {
  public LlvmLoweringPass(LcbConfiguration configuration) {
    super(configuration);
  }

  /**
   * This record contains the basic information for lowering {@link Instruction} and
   * {@link PseudoInstruction}.
   */
  public record BaseInstructionInfo(List<TableGenInstructionOperand> inputs,
                                    List<TableGenInstructionOperand> outputs,
                                    LlvmLoweringPass.Flags flags,
                                    List<RegisterRef> uses,
                                    List<RegisterRef> defs) {
    /**
     * Find the index in the {@link #inputs} by the given field.
     */
    public int findInputIndex(Format.Field field) {
      for (int i = 0; i < inputs.size(); i++) {
        if (inputs.get(i) instanceof ReferencesFormatField x && x.formatField().equals(field)) {
          return i;
        }
      }

      throw Diagnostic.error("Cannot find field in inputs.", field.sourceLocation()).build();
    }

    public BaseInstructionInfo withFlags(LlvmLoweringPass.Flags newFlags) {
      return new BaseInstructionInfo(inputs, outputs, newFlags, uses, defs);
    }
  }

  /**
   * A {@link TableGenInstruction} has many boolean flags which are required for the
   * code generation.
   */
  public record Flags(boolean isTerminator, boolean isBranch, boolean isCall, boolean isReturn,
                      boolean isPseudo, boolean isCodeGenOnly, boolean mayLoad, boolean mayStore,
                      boolean isBarrier, boolean isRematerialisable, boolean isAsCheapAsAMove) {
    public static Flags empty() {
      return new Flags(false, false, false, false, false, false, false, false, false, false, false);
    }

    /**
     * Given {@link Flags} overwrite the {@code isTerminator} and return it.
     */
    public static Flags withTerminator(Flags flags) {
      return new Flags(true, flags.isBranch, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove);
    }

    /**
     * Given {@link Flags} overwrite the {@code isTerminator} and return it.
     */
    public static Flags withNoTerminator(Flags flags) {
      return new Flags(false, flags.isBranch, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove);
    }

    /**
     * Given {@link Flags} overwrite the {@code isBranch} and return it.
     */
    public static Flags withBranch(Flags flags) {
      return new Flags(flags.isTerminator(), true, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable,
          flags.isAsCheapAsAMove);
    }

    /**
     * Given {@link Flags} overwrite the {@code isPseudo} and return it.
     */
    public static Flags withPseudo(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn, true,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable,
          flags.isAsCheapAsAMove);
    }

    /**
     * Given {@link Flags} overwrite the {@code isBarrier} and return it.
     */
    public static Flags withBarrier(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), true, flags.isRematerialisable,
          flags.isAsCheapAsAMove);
    }

    /**
     * Given {@link Flags} overwrite the {@code isBranch} and return it.
     */
    public static Flags withNoBranch(Flags flags) {
      return new Flags(flags.isTerminator(), false, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove);
    }

    /**
     * Given {@link Flags} overwrite the {@code isRematerialisable} and return it.
     */
    public static Flags withIsRematerialisable(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          true, flags.isAsCheapAsAMove);
    }


    /**
     * Given {@link Flags} overwrite the {@code isAsCheapAsMove} and return it.
     */
    public static Flags withIsAsCheapAsMove(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, true);
    }
  }

  /**
   * This is the result of the {@link LlvmLoweringPass}. It contains the
   * tablegen records for machine instructions and pseudo instructions.
   */
  public record LlvmLoweringPassResult(
      IdentityHashMap<Instruction, LlvmLoweringRecord> machineInstructionRecords,
      IdentityHashMap<PseudoInstruction, LlvmLoweringPseudoRecord> pseudoInstructionRecords) {

  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var labelingResult = ensureNonNull(
        (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.sourceLocation()));
    var labelingResultPseudo = ensureNonNull(
        (IsaPseudoInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.sourceLocation()));
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    var architectureType =
        ensurePresent(ValueType.from(abi.stackPointer().registerFile().resultType()),
            "Architecture type is required.");
    var machineStrategies =
        List.of(new LlvmInstructionLoweringAddImmediateStrategyImpl(architectureType),
            new LlvmInstructionLoweringDivisionAndRemainderStrategyImpl(architectureType),
            new LlvmInstructionLoweringLessThanSignedConditionalsStrategyImpl(architectureType),
            new LlvmInstructionLoweringLessThanUnsignedConditionalsStrategyImpl(architectureType),
            new LlvmInstructionLoweringLessThanImmediateUnsignedConditionalsStrategyImpl(
                architectureType),
            new LlvmInstructionLoweringUnconditionalJumpsStrategyImpl(architectureType),
            new LlvmInstructionLoweringConditionalBranchesStrategyImpl(architectureType),
            new LlvmInstructionLoweringIndirectJumpStrategyImpl(architectureType),
            new LlvmInstructionLoweringMemoryStoreStrategyImpl(architectureType),
            new LlvmInstructionLoweringMemoryLoadStrategyImpl(architectureType),
            new LlvmInstructionLoweringXoriAndOriStrategyImpl(architectureType),
            new LlvmInstructionLoweringLoadUpperImmediateStrategyImpl(architectureType),
            new LlvmInstructionLoweringDefaultStrategyImpl(architectureType));
    var pseudoStrategies =
        List.of(new LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl(machineStrategies),
            new LlvmPseudoInstructionLoweringDefaultStrategyImpl(machineStrategies));

    var machineRecords = generateRecordsForMachineInstructions(viam, abi, machineStrategies,
        labelingResult);
    var pseudoRecords = pseudoInstructions(machineRecords, viam, fieldUsages, abi,
        pseudoStrategies, labelingResult, labelingResultPseudo);

    return new LlvmLoweringPassResult(machineRecords, pseudoRecords);
  }


  private IdentityHashMap<Instruction, LlvmLoweringRecord> generateRecordsForMachineInstructions(
      Specification viam, Abi abi,
      List<LlvmInstructionLoweringStrategy> strategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions) {
    var tableGenRecords = new IdentityHashMap<Instruction, LlvmLoweringRecord>();

    viam.isa().map(isa -> isa.ownInstructions().stream()).orElseGet(Stream::empty)
        .forEach(instruction -> {
          var instructionLabel = labelledMachineInstructions.reverse().get(instruction);

          for (var strategy : strategies) {
            if (!strategy.isApplicable(instructionLabel)) {
              // Try next strategy
              continue;
            }

            var record =
                strategy.lowerInstruction(labelledMachineInstructions, instruction,
                    instruction.behavior(),
                    abi);

            // Okay, we have to save record.
            record.ifPresent(llvmLoweringIntermediateResult -> {
              tableGenRecords.put(instruction,
                  llvmLoweringIntermediateResult);

              // Also attach it as extension to the instruction.
              instruction.attachExtension(
                  new TableGenInstructionCtx(llvmLoweringIntermediateResult));
            });

            // Allow only one strategy to apply.
            // Otherwise, the results from a previous strategy are overwritten.
            break;
          }
        });

    return tableGenRecords;
  }

  private IdentityHashMap<PseudoInstruction, LlvmLoweringPseudoRecord> pseudoInstructions(
      IdentityHashMap<Instruction, LlvmLoweringRecord> machineRecords,
      Specification viam,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Abi abi,
      List<LlvmPseudoInstructionLowerStrategy> pseudoStrategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      IsaPseudoInstructionMatchingPass.Result labelledPseudoInstructions
  ) {
    var tableGenRecords = new IdentityHashMap<PseudoInstruction, LlvmLoweringPseudoRecord>();

    viam.isa().map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .forEach(pseudo -> {
          for (var strategy : pseudoStrategies) {
            var label = labelledPseudoInstructions.reverse().get(pseudo);
            if (!strategy.isApplicable(label)) {
              continue;
            }

            var instAliases = instAliases(machineRecords, fieldUsages, pseudo);
            var record =
                strategy.lowerInstruction(abi, instAliases, pseudo, labelledMachineInstructions);

            // Okay, we have to save record.
            record.ifPresent(llvmLoweringIntermediateResult -> tableGenRecords.put(pseudo,
                llvmLoweringIntermediateResult));

            break;
          }
        });

    return tableGenRecords;
  }

  private @Nonnull List<TableGenInstAlias> instAliases(
      IdentityHashMap<Instruction, LlvmLoweringRecord> machineRecords,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      PseudoInstruction pseudo) {
    if (pseudo.behavior().getNodes(InstrCallNode.class).toList().size() != 1) {
      return Collections.emptyList();
    }

    var instruction =
        ensurePresent(pseudo.behavior().getNodes(InstrCallNode.class).findFirst(),
            "must exist");
    var machineRecord = ensureNonNull(machineRecords.get(instruction.target()), "must exist");
    var args = getArgsForInstAlias(machineRecord, fieldUsages, instruction);
    var graph = new Graph("output");
    graph.addWithInputs(
        new LcbMachineInstructionNode(new NodeList<>(args), instruction.target()));

    var instAliases = List.of(
        new TableGenInstAlias(
            pseudo,
            pseudo.assembly(),
            graph
        )
    );
    return instAliases;
  }

  private List<ExpressionNode> getArgsForInstAlias(
      LlvmLoweringRecord machineRecord,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      InstrCallNode instruction) {
    var args = new ArrayList<ExpressionNode>();

    for (int i = 0; i < instruction.arguments().size(); i++) {
      var field = instruction.getParamFields().get(i);
      var argument = instruction.getArgument(field);
      var fieldUsageMap = fieldUsages.getFieldUsages(instruction.target());

      if (Optional.ofNullable(fieldUsageMap.get(field))
          .map(x -> x == IdentifyFieldUsagePass.FieldUsage.REGISTER).orElse(false)) {
        // There are two cases:
        // First, the given argument is `FuncParamNode`. This means that the argument remains
        // a register file.
        // Second, the given argument is a `ConstantNode`. This means that the argument will be
        // a fixed register in a register file.
        if (argument instanceof FuncParamNode) {
          // it is a register file
          var registerFile =
              ensurePresent(
                  instruction.target().behavior().getNodes(FieldRefNode.class)
                      .flatMap(x -> x.usages().filter(y -> y instanceof HasRegisterFile)
                          .map(y -> ((HasRegisterFile) y).registerFile()))
                      .findFirst(), () -> Diagnostic.error("Expected to find register file",
                      field.sourceLocation()));
          args.add(new LcbMachineInstructionParameterNode(
              new TableGenInstructionOperand(null, registerFile.identifier.simpleName(),
                  field.identifier.simpleName())));
        } else if (argument instanceof ConstantNode constantNode) {
          // it is indexed in a register file
          var registerFile =
              ensurePresent(
                  instruction.target().behavior().getNodes(FieldRefNode.class)
                      .flatMap(x -> x.usages().filter(y -> y instanceof HasRegisterFile)
                          .map(y -> ((HasRegisterFile) y).registerFile()))
                      .findFirst(), () -> Diagnostic.error("Expected to find register file",
                      field.sourceLocation()));
          args.add(new LcbMachineInstructionParameterNode(
              new TableGenInstructionOperand(null,
                  registerFile.generateName(constantNode.constant().asVal()))));
        }
      } else {
        // There are two cases:
        // First, the given argument is `FuncCallNode`. This means that the argument remains
        // an immediate which needs to be selected during instruction selection.
        // Second, the given argument is a `ConstantNode`. This means that the argument will be
        // a fixed constant.
        if (argument instanceof FuncParamNode) {
          var fieldAccess =
              ensurePresent(
                  instruction.target().behavior().getNodes(FieldAccessRefNode.class)
                      .filter(x ->
                          x.fieldAccess().fieldRef().equals(field))
                      .findFirst(),
                  () -> Diagnostic.error("Cannot find field access function for field",
                      field.sourceLocation()));
          var operand =
              ensurePresent(
                  Stream.concat(machineRecord.info().inputs().stream(),
                          machineRecord.info().outputs().stream())
                      .filter(x -> (x instanceof TableGenInstructionImmediateOperand y
                          &&
                          y.immediateOperand().fieldAccessRef().equals(fieldAccess.fieldAccess()))
                          || (x instanceof TableGenInstructionImmediateLabelOperand z
                          &&
                          z.immediateOperand().fieldAccessRef().equals(fieldAccess.fieldAccess()))
                      )
                      .findFirst(),
                  () -> Diagnostic.error("Cannot find operand", argument.sourceLocation()));

          args.add(new LcbMachineInstructionParameterNode(operand));
        } else if (argument instanceof ConstantNode constantNode) {
          args.add(new LcbMachineInstructionParameterNode(
              new TableGenInstructionOperand(null, constantNode.constant().asVal().intValue() + "")
          ));
        }
      }
    }

    return args;
  }

  /**
   * The {@link IsaMachineInstructionMatchingPass} computes a hashmap with the instruction label
   * as a key and all the matched instructions as value.
   * However, we would like to check whether {@link LlvmInstructionLoweringStrategy} supports this
   * {@link Instruction} in this pass. That's why we have the flip the hashmap.
   */
  public static IdentityHashMap<Instruction, MachineInstructionLabel> flipMachineInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatched) {
    IdentityHashMap<Instruction, MachineInstructionLabel> inverse = new IdentityHashMap<>();

    for (var entry : isaMatched.entrySet()) {
      for (var item : entry.getValue()) {
        inverse.put(item, entry.getKey());
      }
    }

    return inverse;
  }
}
