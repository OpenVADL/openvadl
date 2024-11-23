package vadl.dump.infoEnrichers;

import static java.util.Objects.requireNonNull;
import static vadl.dump.InfoEnricher.forType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.iss.passes.IssReadVarAssignPass;
import vadl.iss.passes.IssVariableAllocationPass;
import vadl.iss.passes.safeResourceRead.IssSafeResourceReadPass;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * A collection of info enrichers that provide information during the ISS generation.
 */
public class IssEnricherCollection {

  /**
   * An {@link InfoEnricher} that enriches a {@link DefinitionEntity} by adding information
   * about variable assignments and resource reads. Specifically, it processes entities of type
   * {@link DefinitionEntity} and extracts data if the corresponding pass {@link IssVariableAllocationPass}
   * has been executed.
   */
  public static InfoEnricher TCG_VAR_ASSIGN_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        if (!passResult.hasRunPassOnce(IssVariableAllocationPass.class)
            || !(entity.origin() instanceof Instruction instr)) {
          return;
        }

        var assignments = passResult.lastResultOf(
            IssVariableAllocationPass.class,
            IssVariableAllocationPass.Result.class
        );


        var nodes = new ArrayList<String>();
        var vars = new ArrayList<String>();

        requireNonNull(assignments.varAssignments().get(instr))
            .forEach((k, v) -> {
              nodes.add(k.toString());
              vars.add(v.toString());
            });

        nodes.add(0, "Nodes");
        vars.add(0, "Variables");

        var info = InfoUtils.createTableExpandable(
            "Variable Assignments",
            List.of(nodes, vars)
        );
        entity.addInfo(info);
      });


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
      TCG_VAR_ASSIGN_EXPANDABLE,
      READ_SPILL_LOCATION_EXPANDABLE
  );

}
