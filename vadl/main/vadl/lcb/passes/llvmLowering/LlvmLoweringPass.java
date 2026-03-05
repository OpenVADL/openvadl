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
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.RegisterRef;
import vadl.gcb.passes.operands.ReferencesFormatField;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.TableGenInstructionCtx;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmCompilerInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringAddImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesWithStatusRegistersStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringIndirectJumpAndLinkStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringLoadUpperImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryLoadStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryStoreStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringUnconditionalIndirectJumpWithoutLinkRegistersStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringUnconditionalJumpWithLinkRegistersStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringUnconditionalJumpWithoutLinkRegistersStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringXoriAndOriStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringLoadGlobalAddressStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanImmediateUnsignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanSignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals.LlvmInstructionLoweringLessThanUnsignedConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.ReferencesImmediateOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.CompilerInstruction;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This is a wrapper class which contains utility functions for the lowering.
 */
public class LlvmLoweringPass extends Pass {

  private boolean generatePatterns;

  public LlvmLoweringPass(LcbConfiguration configuration) {
    super(configuration);
    this.generatePatterns = !((LcbConfiguration) configuration()).skipPatternGeneration();
  }

  /**
   * This record contains the basic information for lowering {@link Instruction} and
   * {@link PseudoInstruction}.
   */
  public record BaseInstructionInfo(List<GcbInstructionOperand> inputs,
                                    List<GcbInstructionOperand> outputs,
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
        if (inputs.get(i) instanceof ReferencesFormatField x && x.referencesField(field)) {
          return i;
        }
      }

      throw Diagnostic.error("Cannot find field in inputs.", field.location()).build();
    }

    /**
     * Return the concatenation of {@link #outputs} and {@link #inputs} in that order.
     */
    public List<GcbInstructionOperand> outputInputOperands() {
      var result = new ArrayList<>(outputs);
      result.addAll(inputs);
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
                      boolean isBarrier, boolean isRematerialisable, boolean isAsCheapAsAMove,
                      boolean hasSideEffects) {
    public static Flags empty() {
      return new Flags(false, false, false, false, false, false, false, false, false, false, false,
          false);
    }

    /**
     * Given {@link Flags} overwrite the {@code isTerminator} and return it.
     */
    public static Flags withTerminator(Flags flags) {
      return new Flags(true, flags.isBranch, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isTerminator} and return it.
     */
    public static Flags withNoTerminator(Flags flags) {
      return new Flags(false, flags.isBranch, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isBranch} and return it.
     */
    public static Flags withBranch(Flags flags) {
      return new Flags(flags.isTerminator(), true, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable,
          flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isPseudo} and return it.
     */
    public static Flags withPseudo(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn, true,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable,
          flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isBarrier} and return it.
     */
    public static Flags withBarrier(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), true, flags.isRematerialisable,
          flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isBranch} and return it.
     */
    public static Flags withNoBranch(Flags flags) {
      return new Flags(flags.isTerminator(), false, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isRematerialisable} and return it.
     */
    public static Flags withIsRematerialisable(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          true, flags.isAsCheapAsAMove, flags.hasSideEffects);
    }


    /**
     * Given {@link Flags} overwrite the {@code isAsCheapAsMove} and return it.
     */
    public static Flags withIsAsCheapAsMove(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, true, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code isCodeGenOnly} to false.
     */
    public static Flags withNoCodeGenOnly(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo,
          false, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove, flags.hasSideEffects);
    }


    /**
     * Given {@link Flags} overwrite the {@code mayLoad} to true.
     */
    public static Flags withMayLoad(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo, flags.isCodeGenOnly, true, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove, flags.hasSideEffects);
    }

    /**
     * Given {@link Flags} overwrite the {@code hasSideEffects} to true.
     */
    public static Flags withSideEffects(Flags flags) {
      return new Flags(flags.isTerminator(), flags.isBranch, flags.isCall, flags.isReturn,
          flags.isPseudo, flags.isCodeGenOnly, flags.mayLoad, flags.mayStore(), flags.isBarrier,
          flags.isRematerialisable, flags.isAsCheapAsAMove, true);
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
    var registerDefsUses =
        (DetermineRegisterUsesAndDefsPass.Output) passResults.lastResultOf(
            DetermineRegisterUsesAndDefsPass.class);
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
            new LlvmInstructionLoweringUnconditionalJumpWithoutLinkRegistersStrategyImpl(
                architectureType),
            new LlvmInstructionLoweringUnconditionalJumpWithLinkRegistersStrategyImpl(
                architectureType),
            new LlvmInstructionLoweringUnconditionalIndirectJumpWithoutLinkRegistersStrategyImpl(
                architectureType),
            new LlvmInstructionLoweringConditionalBranchesStrategyImpl(architectureType),
            new LlvmInstructionLoweringConditionalBranchesWithStatusRegistersStrategyImpl(
                architectureType),
            new LlvmInstructionLoweringIndirectJumpAndLinkStrategyImpl(architectureType),
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

    var machineRecords = machineInstructions(viam,
        abi,
        registerDefsUses.machineInstructions(),
        machineStrategies,
        labelingResult
    );
    var pseudoRecords = pseudoInstructions(viam, abi, pseudoStrategies, labelingResult,
        labelingResultPseudo, registerDefsUses.pseudoInstructions());
    var compilerInstructions =
        compilerInstructions(abi, compilerStrategies, labelingResult,
            registerDefsUses.compilerInstructions());

    return new LlvmLoweringPassResult(machineRecords, pseudoRecords, compilerInstructions);
  }

  private IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructions(
      Specification viam,
      Abi abi,
      Map<Instruction, DetermineRegisterUsesAndDefsPass.Info> registerDefsUses,
      List<LlvmInstructionLoweringStrategy> strategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions) {
    var tableGenRecords = new IdentityHashMap<Instruction, LlvmLoweringRecord.Machine>();

    viam.isa().stream().flatMap(isa -> isa.ownInstructions().stream())
        .forEach(instruction -> {
          var instructionLabel = labelledMachineInstructions.reverse().get(instruction);
          for (var strategy : strategies) {
            if (!strategy.isApplicable(instructionLabel)) {
              // Try next strategy
              continue;
            }

            var usesDefs = ensureNonNull(registerDefsUses.get(instruction),
                () -> Diagnostic.error("No defs / uses found", instruction.location()));

            var record =
                strategy.lowerInstruction(labelledMachineInstructions,
                    instruction,
                    instruction.behavior(),
                    abi,
                    usesDefs,
                    generatePatterns);

            // Okay, we have to save record.
            record.ifPresent(llvmLoweringIntermediateResult -> {
              tableGenRecords.put(instruction, llvmLoweringIntermediateResult);

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
      Specification viam,
      Abi abi,
      List<LlvmPseudoInstructionLowerStrategy> pseudoStrategies,
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      IsaPseudoInstructionMatchingPass.Result labelledPseudoInstructions,
      Map<PseudoInstruction, DetermineRegisterUsesAndDefsPass.Info> registerDefsUses
  ) {
    var tableGenRecords = new IdentityHashMap<PseudoInstruction, LlvmLoweringRecord.Pseudo>();

    viam.isa().stream().flatMap(isa -> isa.ownPseudoInstructions().stream())
        .forEach(pseudo -> {
          var info = Objects.requireNonNull(registerDefsUses.get(pseudo));
          for (var strategy : pseudoStrategies) {
            var label = labelledPseudoInstructions.reverse().get(pseudo);
            if (!strategy.isApplicable(label, pseudo)) {
              continue;
            }

            var record =
                strategy.lowerInstruction(abi,
                    Collections.emptyList(),
                    pseudo,
                    labelledMachineInstructions,
                    info,
                    generatePatterns);

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
      IsaMachineInstructionMatchingPass.Result labelledMachineInstructions,
      Map<CompilerInstruction, DetermineRegisterUsesAndDefsPass.Info> registerDefsUses) {
    var tableGenRecords = new IdentityHashMap<CompilerInstruction, LlvmLoweringRecord.Compiler>();

    Stream.concat(abi.constantSequences().stream(), abi.registerAdjustmentSequences().stream())
        .forEach(compilerInstruction -> {
          var info = Objects.requireNonNull(registerDefsUses.get(compilerInstruction));
          for (var strategy : compilerStrategies) {
            var record =
                strategy.lowerInstruction(compilerInstruction,
                    labelledMachineInstructions,
                    info
                );

            record.ifPresent(
                llvmLoweringIntermediateResult -> tableGenRecords.put(compilerInstruction,
                    llvmLoweringIntermediateResult));

            break;
          }
        });

    return tableGenRecords;
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
