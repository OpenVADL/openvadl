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

import java.util.ArrayList;
import java.util.List;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.VdtEntity;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtInputPreparationPass;
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

        var dot = new DotGraphGenerator(entity.tree()).generate();

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

        var graph = new TextGraphGenerator(entity.tree()).generate();
        var info = InfoUtils.createCodeBlockExpandable(
            "Decode Tree (Text)",
            graph.toString()
        );

        entity.addInfo(info);
      });

  public static InfoEnricher VDT_TXT_TABLE_EXPANDABLE =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var graph = new InsnDecisionTableGenerator(entity.tree()).generate();
        var info = InfoUtils.createTableExpandable(
            "Decisions by instruction",
            graph
        );

        entity.addInfo(info);
      });

  @SuppressWarnings("unchecked")
  public static InfoEnricher VDT_STATS_EXPANDABLE =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var stats = DecisionTreeStatsCalculator.statistics(entity.tree());

        final var statsTable = new ArrayList<List<String>>();

        final List<String> categories = new ArrayList<>(
            List.of("Property", "Number of Nodes", "Number of Instructions",
                "Number of Leaves", "Minimum Depth", "Maximal Depth", "Average Depth"));
        if (stats.getOccurrenceProbability() > 0.0) {
          categories.add("Weighted Average Depth");
        }
        categories.add("Longest instruction width");
        statsTable.add(categories);

        final List<String> values = new ArrayList<>(
            List.of("Value", String.valueOf(stats.getNumberOfNodes()),
                String.valueOf(getInsnCount(passResults)),
                String.valueOf(stats.getNumberOfLeafNodes()), String.valueOf(stats.getMinDepth()),
                String.valueOf(stats.getMaxDepth()),
                String.valueOf(Math.round(stats.getAvgDepth() * 100) / 100.0))
        );
        if (stats.getOccurrenceProbability() > 0.0) {
          values.add(String.valueOf(Math.round(stats.getWeightedAvgDepth() * 100) / 100.0));
        }
        values.add(stats.getMaxInstructionWidth() + " bit");

        statsTable.add(values);

        var info = InfoUtils.createTableExpandable("Statistics", statsTable);
        entity.addInfo(info);
      });

  public static InfoEnricher VDT_STATS_TAGS =
      InfoEnricher.forType(VdtEntity.class, (entity, passResults) -> {

        var stats = DecisionTreeStatsCalculator.statistics(entity.tree());

        entity.addInfo(Info.Tag.of("Instructions", String.valueOf(getInsnCount(passResults))));
        entity.addInfo(Info.Tag.of("Nodes", String.valueOf(stats.getNumberOfNodes())));
        entity.addInfo(Info.Tag.of("Leaves", String.valueOf(stats.getNumberOfLeafNodes())));
        entity.addInfo(Info.Tag.of("Max Depth", String.valueOf(stats.getMaxDepth())));
        entity.addInfo(Info.Tag.of("Avg Depth",
            String.valueOf(Math.round(stats.getAvgDepth() * 100) / 100.0)));

        if (stats.getOccurrenceProbability() > 0.0) {
          entity.addInfo(Info.Tag.of("Weighted Avg Depth",
              String.valueOf(Math.round(stats.getWeightedAvgDepth() * 100) / 100.0)));
        }
      });

  @SuppressWarnings("unchecked")
  private static int getInsnCount(PassResults passResults) {
    final List<DecodeEntry> entries;
    if (passResults.hasRunPassOnce(VdtConstraintSynthesisPass.class)) {
      entries =
          (List<DecodeEntry>) passResults.lastNullableResultOf(
              VdtConstraintSynthesisPass.class);
    } else {
      entries =
          (List<DecodeEntry>) passResults.lastNullableResultOf(VdtInputPreparationPass.class);
    }
    return entries != null ? entries.size() : 0;
  }

  public static List<InfoEnricher> all = List.of(
      VDT_STATS_TAGS,
      VDT_STATS_EXPANDABLE,
      VDT_TXT_GRAPH_EXPANDABLE,
      VDT_DOT_GRAPH_MODAL_ENRICHER,
      VDT_TXT_TABLE_EXPANDABLE
  );

}
