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

package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.iss.passes.safeResourceRead.IssSafeResourceReadPass;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * A collection of info enrichers that provide information during the ISS generation.
 */
public class IssEnricherCollection {

  /**
   * Obtains, the locations of register spilling (saving in to copy node).
   */
  public static InfoEnricher READ_SPILL_LOCATION_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        if (!passResult.hasRunPassOnce(IssSafeResourceReadPass.class)
            || !(entity.origin() instanceof Instruction instr)) {
          return;
        }


        var result = passResult.lastResultOf(
            IssSafeResourceReadPass.class,
            IssSafeResourceReadPass.Result.class
        );
        var instrReads =
            instr.behavior().getNodes(ReadResourceNode.class).collect(Collectors.toSet());
        var entries = result.readTempSpillLocations().entrySet().stream()
            .filter(e -> instrReads.contains(e.getKey()))
            .toList();
        var reads = entries.stream().map(e -> e.getKey().toString())
            .collect(Collectors.toCollection(ArrayList::new));
        var locations = entries.stream().map(e -> e.getValue().toString())
            .collect(Collectors.toCollection(ArrayList::new));

        if (reads.isEmpty()) {
          return;
        }

        reads.add(0, "Reads");
        locations.add(0, "Location");

        var info = InfoUtils.createTableExpandable(
            "Conflicting Read Spill Locations",
            List.of(reads, locations)
        );
        entity.addInfo(info);
      });

  /**
   * A list of all info enrichers that are ISS specific.
   */
  public static List<InfoEnricher> all = List.of(
      READ_SPILL_LOCATION_EXPANDABLE
  );

}
