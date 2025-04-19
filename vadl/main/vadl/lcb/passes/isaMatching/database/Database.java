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

package vadl.lcb.passes.isaMatching.database;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.PseudoInstructionLabel;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.pass.PassResults;
import vadl.utils.SourceLocation;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This database contains the labelled instructions and pseudo instructions and makes it possible
 * to query for instructions.
 */
public class Database {
  private final Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions;
  private final Map<PseudoInstructionLabel, List<PseudoInstruction>> labelledPseudoInstructions;

  /**
   * Constructor. It requires the information from {@link IsaMachineInstructionMatchingPass} and
   * {@link IsaPseudoInstructionMatchingPass} to have labelled instructions and pseudo instructions.
   */
  public Database(PassResults passResults, Specification viam) {
    var labelingResult = ensureNonNull(
        (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.location()));
    var labelingPseudoResult = ensureNonNull(
        (IsaPseudoInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.location()));
    this.labelledMachineInstructions = labelingResult.labels();
    this.labelledPseudoInstructions = labelingPseudoResult.labels();
  }

  /**
   * Constructor for {@link Database}.
   */
  public Database(IsaMachineInstructionMatchingPass.Result labelingResult) {
    this.labelledMachineInstructions = labelingResult.labels();
    this.labelledPseudoInstructions = Collections.emptyMap();
  }

  /**
   * Run the given {@link Query} and return the matched {@link Instruction} and
   * {@link PseudoInstruction} wrapped by {@link QueryResult}.
   * Note that the query will be executed on both types of instructions. Therefore, when
   * given a {@link MachineInstructionLabel} and {@link PseudoInstructionLabel} then you might get
   * two results and not the intersection of both.
   */
  public QueryResult run(Query query) {
    var result = matchInstructions(query);

    return new QueryResult(query, result.machineInstructions(), result.pseudoInstructions());
  }

  private QueryResult matchInstructions(Query query) {
    var resultMachineInstructions = new ArrayList<Instruction>();
    var resultPseudoInstructions = new ArrayList<PseudoInstruction>();

    if (query.machineInstructionLabel() != null) {
      resultMachineInstructions.addAll(
          labelledMachineInstructions.getOrDefault(query.machineInstructionLabel(),
              Collections.emptyList()));
    }

    if (query.pseudoInstructionLabel() != null) {
      resultPseudoInstructions.addAll(
          labelledPseudoInstructions.getOrDefault(query.pseudoInstructionLabel(),
              Collections.emptyList()));
    }

    if (query.machineInstructionLabelGroup() != null) {
      var labels = query.machineInstructionLabelGroup().labels();
      for (var label : labels) {
        var instruction = labelledMachineInstructions.get(label);
        if (instruction != null) {
          resultMachineInstructions.addAll(instruction);
        }
      }
    }

    for (var x : query.or()) {
      var subResult = matchInstructions(x);
      resultMachineInstructions.addAll(subResult.machineInstructions());
      resultPseudoInstructions.addAll(subResult.pseudoInstructions());
    }

    for (var x : query.withBehavior()) {
      // Remove machine instruction when any behavior query matches.
      resultMachineInstructions.removeIf(instruction -> {
        var satisfied = instruction.behavior().getNodes()
            .filter(node -> x.applicable().isInstance(node))
            .allMatch(node -> x.predicate().test(node));

        // only remove when it's not covered.
        return !satisfied;
      });
    }

    return new QueryResult(query, resultMachineInstructions, resultPseudoInstructions);
  }


  /**
   * The compiler generator has a pass which tries to assign {@link MachineInstructionLabel} for
   * an {@link Instruction}. This is useful when we want to find an {@link Instruction} with
   * a certain property. However, in some cases, we need to do opposite. We have an
   * {@link Instruction} and require the {@link MachineInstructionLabel}. This method flips the
   * matched {@link Map}.
   */
  public IdentityHashMap<Instruction, MachineInstructionLabel> flipMachineInstructions() {
    return LlvmLoweringPass.flipMachineInstructions(labelledMachineInstructions);
  }

  /**
   * This is a short-cut method because finding an addition-immediate instruction is so common.
   *
   * @return a 64 bit addi instruction or a 32 bit addi instruction (when 64 does not exist).
   * @throws Diagnostic when both do not exist.
   */
  public Instruction getAddImmediate() {
    var query =
        new Query.Builder().machineInstructionLabel(MachineInstructionLabel.ADDI_32).build();
    var addi32 = run(query);

    if (addi32.machineInstructions().isEmpty()) {
      var query2 =
          new Query.Builder().machineInstructionLabel(MachineInstructionLabel.ADDI_64).build();
      var addi64 = run(query2);

      if (!addi64.machineInstructions().isEmpty()) {
        return addi64.firstMachineInstruction();
      } else {
        throw Diagnostic.error("There is no addition immediate instruction",
            SourceLocation.INVALID_SOURCE_LOCATION).build();
      }
    } else {
      return addi32.firstMachineInstruction();
    }

  }
}
