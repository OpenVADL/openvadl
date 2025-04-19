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
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.TableGenInstructionCtx;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmCompilerInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringAddImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringIndirectJumpStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringLoadUpperImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryLoadStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryStoreStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringXoriAndOriStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringLoadGlobalAddressStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanImmediateUnsignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanSignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanUnsignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.CompilerInstruction;
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
     * Get the input operands which are immediates.
     */
    public List<ReferencesImmediateOperand> inputImmediates() {
      return inputs.stream()
          .filter(x -> x instanceof ReferencesImmediateOperand)
          .map(x -> (ReferencesImmediateOperand) x)
          .toList();
    }

    /**
     * Find the index in the {@link #inputs} by the given field.
     */
    public int findInputIndex(Format.Field field) {
      for (int i = 0; i < inputs.size(); i++) {
        if (inputs.get(i) instanceof ReferencesFormatField x && x.formatField().equals(field)) {
          return i;
        }
      }

      throw Diagnostic.error("Cannot find field in inputs.", field.location()).build();
    }

    /**
     * Return the concatenation of {@link #outputs} and {@link #inputs} in that order.
     */
    public List<TableGenInstructionOperand> outputInputOperands() {
      var result = new ArrayList<>(outputs);
      result.addAll(inputs);
      return result;
    }

    /**
     * Return the format fields of the {@link #outputs} and {@link #inputs}.
     *
     * @throws Diagnostic if any operand is not a {@link ReferencesFormatField}.
     */
    public List<Format.Field> outputInputOperandsFormatFields() {
      var result = new ArrayList<Format.Field>();
      for (var operand : outputInputOperands()) {
        if (operand instanceof ReferencesFormatField x) {
          result.add(x.formatField());
        } else {
          throw Diagnostic.error("Expected to find format field on operand.",
              SourceLocation.INVALID_SOURCE_LOCATION).build();
        }
      }
      return result;
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

    /**
     * Given {@link Flags} overwrite the {@code isCodeGenOnly} to false.
     */
    public static Flags withNoCodeGenOnly(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          false, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove);
    }


    /**
     * Given {@link Flags} overwrite the {@code mayLoad} to true.
     */
    public static Flags withMayLoad(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo, flags.isCodeGenOnly, true, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove);
    }
  }

  /**
   * This is the result of the {@link LlvmLoweringPass}. It contains the
   * tablegen records for machine instructions, pseudo instructions and compiler instructions.
   */
  public record LlvmLoweringPassResult(
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords,
      IdentityHashMap<PseudoInstruction, LlvmLoweringRecord.Pseudo> pseudoInstructionRecords,
      IdentityHashMap<CompilerInstruction, LlvmLoweringRecord.Compiler>
      compilerInstructionRecords) {

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
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.location()));
    var labelingResultPseudo = ensureNonNull(
        (IsaPseudoInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.location()));
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().orElseThrow();

    var architectureType =
        ensurePresent(ValueType.from(abi.stackPointer().registerFile().resultType()),
            "Architecture type is required.");
    var machineStrategies =
        List.of(new LlvmInstructionLoweringAddImmediateStrategyImpl(architectureType),
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
            new LlvmPseudoInstructionLoweringLoadGlobalAddressStrategyImpl(machineStrategies,
                viam.abi().orElseThrow()),
            new LlvmPseudoInstructionLoweringDefaultStrategyImpl(machineStrategies));
    var compilerStrategies =
        List.of(new LlvmCompilerInstructionLoweringDefaultStrategyImpl(machineStrategies));

    var machineRecords = machineInstructions(viam, abi, machineStrategies,
        labelingResult);
    var pseudoRecords = pseudoInstructions(machineRecords, viam, fieldUsages, abi,
        pseudoStrategies, labelingResult, labelingResultPseudo);
    var compilerInstructions =
        compilerInstructions(abi, compilerStrategies, labelingResult);

    return new LlvmLoweringPassResult(machineRecords, pseudoRecords, compilerInstructions);
  }


  private IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructions(
      Specification viam, Abi abi,
      List<LlvmInstructionLoweringStrategy> strategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions) {
    var tableGenRecords = new IdentityHashMap<Instruction, LlvmLoweringRecord.Machine>();

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

  private IdentityHashMap<PseudoInstruction, LlvmLoweringRecord.Pseudo> pseudoInstructions(
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineRecords,
      Specification viam,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      Abi abi,
      List<LlvmPseudoInstructionLowerStrategy> pseudoStrategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      IsaPseudoInstructionMatchingPass.Result labelledPseudoInstructions
  ) {
    var tableGenRecords = new IdentityHashMap<PseudoInstruction, LlvmLoweringRecord.Pseudo>();

    viam.isa().map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .forEach(pseudo -> {
          for (var strategy : pseudoStrategies) {
            var label = labelledPseudoInstructions.reverse().get(pseudo);
            if (!strategy.isApplicable(label, pseudo)) {
              continue;
            }

            var instAliases = instAliases(machineRecords, fieldUsages, pseudo);
            var record =
                strategy.lowerInstruction(abi,
                    instAliases,
                    pseudo,
                    labelledMachineInstructions);

            record.ifPresent(llvmLoweringIntermediateResult -> tableGenRecords.put(pseudo,
                llvmLoweringIntermediateResult));

            break;
          }
        });

    return tableGenRecords;
  }

  private IdentityHashMap<CompilerInstruction, LlvmLoweringRecord.Compiler> compilerInstructions(
      Abi abi,
      List<LlvmCompilerInstructionLoweringDefaultStrategyImpl> compilerStrategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions) {
    var tableGenRecords = new IdentityHashMap<CompilerInstruction, LlvmLoweringRecord.Compiler>();

    Stream.concat(abi.constantSequences().stream(), abi.registerAdjustmentSequences().stream())
        .forEach(compilerInstruction -> {
          for (var strategy : compilerStrategies) {
            var record =
                strategy.lowerInstruction(compilerInstruction,
                    labelledMachineInstructions);

            record.ifPresent(
                llvmLoweringIntermediateResult -> tableGenRecords.put(compilerInstruction,
                    llvmLoweringIntermediateResult));

            break;
          }
        });

    return tableGenRecords;
  }

  private @Nonnull List<TableGenInstAlias> instAliases(
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineRecords,
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

    return List.of(
        new TableGenInstAlias(
            pseudo,
            pseudo.assembly(),
            graph
        )
    );
  }

  private List<ExpressionNode> getArgsForInstAlias(
      LlvmLoweringRecord machineRecord,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      InstrCallNode instruction) {
    var args = new ArrayList<ExpressionNode>();

    for (int i = 0; i < machineRecord.info().outputInputOperandsFormatFields().size(); i++) {
      var field = machineRecord.info().outputInputOperandsFormatFields().get(i);
      var argument = instruction.getArgument(field);
      var fieldUsageMap = fieldUsages.getFieldUsages(instruction.target());

      if (Optional.ofNullable(fieldUsageMap.get(field))
          .map(x -> x.stream().anyMatch(y -> y == IdentifyFieldUsagePass.FieldUsage.REGISTER))
          .orElse(false)) {
        // There are two cases:
        // First, the given argument is `FuncParamNode`. This means that the argument remains
        // a register file.
        // Second, the given argument is a `ConstantNode`. This means that the argument will be
        // a fixed register in a register file.
        if (argument instanceof FuncParamNode funcParamNode) {
          // it is a register file
          var registerFile =
              ensurePresent(
                  instruction.target().behavior().getNodes(FieldRefNode.class)
                      .flatMap(x -> x.usages().filter(y -> y instanceof HasRegisterFile)
                          .map(y -> ((HasRegisterFile) y).registerFile()))
                      .findFirst(), () -> Diagnostic.error("Expected to find register file",
                      field.location()));
          // We use the funcParamNode's name because we need to make sure that the register
          // renaming is handled.
          // pseudo instruction BEQZ( rs : Index, offset : Bits<12> ) =
          //  {
          //      BEQ{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
          //  }
          // Here register `rs1` gets renamed to `rs`.
          // So the pattern will look like:
          // `def : InstAlias<"BEQZ $rs,$offset", (BEQ X:$rs, X0, RV3264I_Btype_immAsLabel:$imm)>;`
          args.add(new LcbMachineInstructionParameterNode(
              new TableGenInstructionOperand(null, registerFile.identifier.simpleName(),
                  funcParamNode.parameter().simpleName())));
        } else if (argument instanceof ConstantNode constantNode) {
          // it is indexed in a register file
          var registerFile =
              ensurePresent(
                  instruction.target().behavior().getNodes(FieldRefNode.class)
                      .flatMap(x -> x.usages().filter(y -> y instanceof HasRegisterFile)
                          .map(y -> ((HasRegisterFile) y).registerFile()))
                      .findFirst(), () -> Diagnostic.error("Expected to find register file",
                      field.location()));
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
                      field.location()));
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
                  () -> Diagnostic.error("Cannot find operand", argument.location()));

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
