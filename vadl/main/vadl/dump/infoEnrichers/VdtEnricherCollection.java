package vadl.dump.infoEnrichers;

import java.util.ArrayList;
import java.util.List;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.VdtEntity;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.target.dump.DotGraphGenerator;
import vadl.vdt.target.dump.InsnDecisionTableGenerator;
import vadl.vdt.target.dump.TextGraphGenerator;

/**
 * A collection of enrichers for VDT entities.
 */
public class VdtEnricherCollection {

  public static InfoEnricher VDT_DOT_GRAPH_MODAL_ENRICHER =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var dot = DotGraphGenerator.generate(entity.tree());

        // Add a modal to the definition entity
        var info = new Info.Modal("Decode Tree (DOT)", "");
        var id = info.id();

        var dotScript = """
            <script id="dot-graph-%s" type="application/dot">
              %s
            </script>
            """.formatted(id, dot);

        var setGraphFunc = """
            <script>
                function setGraph%s(id) {
                    var dotString = document.getElementById(id).textContent;
            
                    // Render the graph
                    d3.select('#graph-%s')
                        .graphviz()
                        .width('100%%')
                        .height('100%%')
                        .renderDot(dotString);
            
                    // Enable pan and zoom (needs a timeout to wait for the graph to render)
                    setTimeout(() => {
                        svgPanZoom(document.querySelector('#graph-%s svg'));
                    }, 100);
                }
            </script>
            """.formatted(id, id, id);

        info.body = """
            <div class="flex flex-col h-full">
                <div id="graph-%s" class="flex-grow rounded-md flex items-center justify-center">
                    <!-- Graph will render here -->
                </div>
            </div>
            %s
            %s
            """.formatted(id, dotScript, setGraphFunc);

        info.jsOnFirstOpen = """
            setGraph%s("dot-graph-%s");
            """.formatted(id, id);

        entity.addInfo(info);
      });

  public static InfoEnricher VDT_TXT_GRAPH_EXPANDABLE =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var graph = TextGraphGenerator.generate(entity.tree());
        var info = InfoUtils.createCodeBlockExpandable(
            "Decode Tree (Text)",
            graph.toString()
        );

        entity.addInfo(info);
      });

  public static InfoEnricher VDT_TXT_TABLE_EXPANDABLE =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var graph = InsnDecisionTableGenerator.generate(entity.tree());
        var info = InfoUtils.createTableExpandable(
            "Decisions by instruction",
            graph
        );

        entity.addInfo(info);
      });

  public static InfoEnricher VDT_STATS_EXPANDABLE =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var stats = DecisionTreeStatsCalculator.statistics(entity.tree());

        final var statsTable = new ArrayList<List<String>>();

        statsTable.add(List.of("Property", "Number of Nodes", "Number of Leaves (Instructions)",
            "Minimum Depth",
            "Maximal Depth", "Average Depth", "Longest instruction width"));
        statsTable.add(List.of("Value", String.valueOf(stats.getNumberOfNodes()),
            String.valueOf(stats.getNumberOfLeafNodes()), String.valueOf(stats.getMinDepth()),
            String.valueOf(stats.getMaxDepth()),
            String.valueOf(Math.round(stats.getAvgDepth() * 100) / 100.0),
            stats.getMaxInstructionWidth() + " bit"));

        var info = InfoUtils.createTableExpandable("Statistics", statsTable);
        entity.addInfo(info);
      });

  public static InfoEnricher VDT_STATS_TAGS =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var stats = DecisionTreeStatsCalculator.statistics(entity.tree());

        entity.addInfo(Info.Tag.of("Instructions", String.valueOf(stats.getNumberOfLeafNodes())));
        entity.addInfo(Info.Tag.of("Nodes", String.valueOf(stats.getNumberOfNodes())));
        entity.addInfo(Info.Tag.of("Max Depth", String.valueOf(stats.getMaxDepth())));
        entity.addInfo(Info.Tag.of("Avg Depth",
            String.valueOf(Math.round(stats.getAvgDepth() * 100) / 100.0)));
      });

  public static List<InfoEnricher> all = List.of(
      VDT_STATS_TAGS,
      VDT_STATS_EXPANDABLE,
      VDT_TXT_GRAPH_EXPANDABLE,
      VDT_DOT_GRAPH_MODAL_ENRICHER,
      VDT_TXT_TABLE_EXPANDABLE
  );

}
