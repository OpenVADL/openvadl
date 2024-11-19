package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.iss.passes.IssReadVarAssignPass;
import vadl.iss.passes.IssTcgAnnotatePass;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.passes.InstructionResourceAccessAnalysisPass;

/**
 * A collection of info enrichers that provide information during the ISS generation.
 */
public class IssEnricherCollection {

  /**
   * An {@link InfoEnricher} that enriches a {@link DefinitionEntity} by adding information
   * about variable assignments and resource reads. Specifically, it processes entities of type
   * {@link DefinitionEntity} and extracts data if the corresponding pass {@link IssReadVarAssignPass}
   * has been executed. The information is added as an expandable table labeled
   * "Accessed Resources" with details on the variables read.
   */
  public static InfoEnricher READ_VAR_ASSIGN_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        if (!passResult.hasRunPassOnce(IssReadVarAssignPass.class)
            || !(entity.origin() instanceof Instruction instr)) {
          return;
        }

        var assignments = passResult.lastResultOf(
            IssReadVarAssignPass.class,
            IssReadVarAssignPass.Result.class
        );
        var reads = Objects.requireNonNull(instr.behavior().getNodes(ReadResourceNode.class))
            .sorted(Comparator.comparing(e -> e.getClass().getSimpleName()))
            .toList();
        var vars = reads.stream().map(assignments.assignments()::get)
            .map(TcgV::varName)
            .collect(Collectors.toCollection(ArrayList::new));

        if (vars.isEmpty()) {
          return;
        }

        var readStr = reads.stream().map(Node::toString)
            .collect(Collectors.toCollection(ArrayList::new));

        readStr.add(0, "Reads");
        vars.add(0, "Variables");

        var info = InfoUtils.createTableExpandable(
            "Read Variable Assignments",
            List.of(readStr, vars)
        );
        entity.addInfo(info);
      });

  /**
   * A list of all info enrichers that are ISS specific.
   */
  public static List<InfoEnricher> all = List.of(
      READ_VAR_ASSIGN_EXPANDABLE
  );

}
