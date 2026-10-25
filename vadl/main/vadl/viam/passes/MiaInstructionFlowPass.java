// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.passes;

import static vadl.error.Diagnostic.error;
import static vadl.error.Diagnostic.warning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.Logic;
import vadl.viam.MiaInstructionFlow;
import vadl.viam.MicroArchitecture;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.dependency.LogicRef;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.OperationRef;
import vadl.viam.graph.dependency.ReadStageOutputNode;

/**
 * Enriches the VIAM's {@link vadl.viam.MicroArchitecture} by constructing a
 * graph modeling the flow of instructions through the MiA's stages.
 */
public class MiaInstructionFlowPass extends Pass {

  public MiaInstructionFlowPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("MiaInstructionFlowPass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {

    if (viam.mia().isEmpty()) {
      return null;
    }

    final var mia = viam.mia().get();

    final var stages = mia.stages();

    final var allInstructionFlows = mia.allInstructionFlows();
    final var instructionFlowsBySource = mia.instructionFlowsBySource();
    final var instructionFlowsByDestination = mia.instructionFlowsByDestination();

    final var filters = new HashMap<MiaInstructionFlow, Set<Instruction>>();

    // Find all instruction flows from stages or reservation stations to
    // stages. This is done by searching each stage's behavior for
    // `ReadStageOutputNode`s and `MiaBuiltinCall`s to `consume` and following
    // them.
    for (var stage : stages) {
      stage.behavior().getNodes().forEach(node -> {
        if (node instanceof ReadStageOutputNode source) {
          allInstructionFlows.add(new MiaInstructionFlow.StageToStage(
              source.stage(), stage, Collections.emptySet()
          ));
        } else if (node instanceof MiaBuiltInCall consume
            && (consume.builtIn() == BuiltInTable.CONSUME_ONE_ARG
             || consume.builtIn() == BuiltInTable.CONSUME_TWO_ARG)
        ) {
          final var source = consume.arguments().getFirst();

          final MiaInstructionFlow flow;

          if (source instanceof LogicRef logicRef) {
            if (!(logicRef.logic() instanceof Logic.ReservationStation rs)) {
              throw new IllegalStateException(
                  "Expected first argument of `consume` to be a `logic [reservation station]`."
              );
            }
            flow = new MiaInstructionFlow.ReservationStationToStage(
                rs, stage, new HashSet<>()
            );
          } else if (source instanceof ReadStageOutputNode stageRef) {
            flow = new MiaInstructionFlow.StageToStage(
                stageRef.stage(), stage, new HashSet<>()
            );
          } else {
            throw new IllegalStateException(
                "Expected first argument to `consume` to reference a logic element."
            );
          }

          if (consume.builtIn() == BuiltInTable.CONSUME_TWO_ARG) {
            final var filter = ((OperationRef) consume.arguments().get(1))
                .operation()
                .getInstructions();

            filters.put(flow, filter);
          }

          allInstructionFlows.add(flow);
        }
      });
    }

    final var rootStages = new HashSet<>(stages);

    final var unusedReservationStations = new HashSet<Logic.ReservationStation>();

    // Find instruction flows from reservation stations to their source stage.
    for (var logic : mia.logic()) {
      if (logic instanceof Logic.ReservationStation rs) {

        final var flow = new MiaInstructionFlow.StageToReservationStation(
            rs.source(), rs, new HashSet<>()
        );

        allInstructionFlows.add(flow);

        final var filter = rs.filter();
        if (filter != null) {
          filters.put(flow, filter.getInstructions());
        }

        unusedReservationStations.add(rs);
      }
    }

    // Construct indices over the instruction flows, both by source and by
    // destination. This facilitates quick access for traversals.
    // At the same time we also find the root stage by excluding all stages
    // that read from others. This should only leave one stage in a well-formed
    // MiA.
    for (var instructionFlow : allInstructionFlows) {
      final var destination = instructionFlow.destination();
      if (destination instanceof Stage destinationStage) {
        rootStages.remove(destinationStage);
      }

      final var source = instructionFlow.source();
      if (source instanceof Logic.ReservationStation rs) {
        unusedReservationStations.remove(rs);
      }

      instructionFlowsBySource.compute(source, (unused, v) -> {
        if (v == null) {
          v = new ArrayList<>();
        }

        v.add(instructionFlow);
        return v;
      });

      instructionFlowsByDestination.compute(destination, (unused, v) -> {
        if (v == null) {
          v = new ArrayList<>();
        }

        v.add(instructionFlow);
        return v;
      });
    }

    if (!unusedReservationStations.isEmpty()) {
      for (var rs : unusedReservationStations) {
        DeferredDiagnosticStore.add(warning("Unused Reservation Station", rs)
            .description("`logic [reservation station] %s` is never read from.", rs.simpleName())
            .build()
        );
      }
    }

    if (rootStages.size() == 1) {
      mia.setRootStage(rootStages.iterator().next());

      final var allInstructions = new HashSet<>(mia.isa().ownInstructions());

      computeReachingInstructions(
          mia, filters, allInstructions, rootStages.iterator().next()
      );
    } else if (stages.isEmpty()) {
      throw error("Micro Architecture Does Not Contain Any Stages", mia)
          .description("At least one initial stage is required.")
          .build();
    } else if (rootStages.isEmpty()) {
      throw error("No Initial Stage Found", mia)
          .description("Could not find any stages that have no inputs.")
          .build();
    } else {
      final var err = error("Multiple Initial Stages Found", mia)
          .description("Found more than one stage that have no inputs.");

      for (var root : rootStages) {
        err.locationDescription(root, "Could be this stage");
      }

      throw err.build();
    }

    return null;
  }

  /**
   * Updates the
   * {@link MiaInstructionFlow#allowedInstructions() allowedInstructions} of
   * all {@link MiaInstructionFlow MiaInstructionFlows} in the given
   * {@link MicroArchitecture mia} by recursively traversing down
   * {@link MiaInstructionFlow MiaInstructionFlows} starting at the root
   * {@link Stage} and applying the relevant filters.
   *
   * @param mia      The {@link MicroArchitecture} whose
   *                 {@link MiaInstructionFlow#allowedInstructions()} fields
   *                 are to be updated.
   * @param filters  A map from {@link MiaInstructionFlow MiaInstructionFlows}
   *                 to sets of {@link Instruction Instructions} that are
   *                 explicitly allowed to pass along them. Note that this is
   *                 not the same as
   *                 {@link MiaInstructionFlow#allowedInstructions()} since
   *                 missing entries indicate that <em>all</em> instructions
   *                 may pass.
   * @param reaching The set of {@link Instruction Instructions} that can reach
   *                 {@code current} from the root stage. This should initially
   *                 contain all Instructions in the implemented
   *                 {@link vadl.viam.InstructionSetArchitecture ISA}.
   * @param current  The {@link Definition} from which the reaching instruction
   *                 flows are to be computed. This should initially be the
   *                 {@code mia}'s root stage.
   */
  private static void computeReachingInstructions(
      MicroArchitecture mia,
      Map<MiaInstructionFlow, Set<Instruction>> filters,
      Set<Instruction> reaching,
      Definition current
  ) {
    final var outflows = mia.instructionFlowsBySource().get(current);

    if (outflows == null) {
      return;
    }

    for (var outflow : outflows) {
      final var filter = filters.get(outflow);
      final var nextReaching = outflow.allowedInstructions();

      if (filter == null) {
        nextReaching.addAll(reaching);
      } else {
        nextReaching.addAll(filter);
        nextReaching.retainAll(reaching);

        // If `current` is reachable, but no instructions can proceed along the current outflow,
        // record a warning.
        if (!reaching.isEmpty() && nextReaching.isEmpty()) {
          DeferredDiagnosticStore.add(
              warning("MiA Element not Reachable by any Instructions", outflow.destination())
                  .description(
                      "Given the filters between MiA elements, there are no instructions in the "
                    + "implemented ISA that can reach `%s` from the root stage `%s`",
                      outflow.destination(),
                      mia.rootStage()
                  ).build()
          );
        }
      }

      outflow.allowedInstructions().addAll(nextReaching);

      computeReachingInstructions(
          mia, filters, nextReaching, outflow.destination()
      );
    }
  }
}
