package vadl.dump.infoEnrichers;

import static java.util.Objects.requireNonNull;
import static vadl.dump.InfoEnricher.forType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.iss.passes.IssTcgVAllocationPass;
import vadl.iss.passes.nodes.TcgVRefNode;
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
