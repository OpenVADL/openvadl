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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.MiaDependency;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.graph.dependency.ReadStageOutputNode;

/**
 * Enriches the VIAM's {@link vadl.viam.MicroArchitecture} by constructing a
 * graph modeling the flow of instructions through the MiA's stages.
 */
public class MiaDependencyPass extends Pass {

  public MiaDependencyPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("MiaDependencyPass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    if (viam.mia().isEmpty()) {
      return null;
    }

    final var mia = viam.mia().get();

    final var stages = mia.stages();

    final var allDependencies = mia.allDependencies();
    final var dependenciesBySource = mia.dependenciesBySource();
    final var dependenciesByDestination = mia.dependenciesByDestination();

    // Find all dependencies from stages on stage outputs. This is done by
    // inspecting each stage's behavior for `ReadStageOutputNode`s and
    // following them.
    for (var stage : stages) {
      stage
          .behavior()
          .getNodes(ReadStageOutputNode.class)
          .map(ReadStageOutputNode::stageOutput)
          .filter(Objects::nonNull)
          .distinct()
          .forEach(read -> allDependencies.add(new MiaDependency.StageToStageOutputDependency(
              read,
              stage
          )));
    }

    final var rootStages = new HashSet<>(stages);

    // Construct indices over the dependencies, both by source and by
    // destination. This facilitates quick access for traversals.
    // At the same time we also find the root stage by excluding all stages
    // that read from others. This should only leave one stage in a well-formed
    // MiA.
    for (var dependency : allDependencies) {
      if (dependency.destination() instanceof Stage destinationStage) {
        rootStages.remove(destinationStage);
      }

      dependenciesBySource.compute(dependency.source(), (unused, v) -> {
        if (v == null) {
          v = new ArrayList<>();
        }

        v.add(dependency);
        return v;
      });

      dependenciesByDestination.compute(dependency.destination(), (unused, v) -> {
        if (v == null) {
          v = new ArrayList<>();
        }

        v.add(dependency);
        return v;
      });
    }

    if (rootStages.size() == 1) {
      mia.setRootStage(rootStages.iterator().next());
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
}
