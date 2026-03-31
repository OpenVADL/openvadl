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

package vadl.rtl.ipg;

import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.Definition;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.visualize.DotGraphVisualizer;

/**
 * Visualize the instruction progress graph as a DOT language graph.
 *
 * <p>Includes the instructions associated with each node in the visualization.
 */
public class InstructionProgressGraphVisualizer extends DotGraphVisualizer {

  private @Nullable InstructionProgressGraph ipg;
  private boolean withInstructions = true;
  private boolean withNameHints = true;

  public InstructionProgressGraphVisualizer withInstructions(boolean option) {
    withInstructions = option;
    return this;
  }

  public InstructionProgressGraphVisualizer withNameHints(boolean option) {
    withNameHints = option;
    return this;
  }

  @Override
  public DotGraphVisualizer load(Graph graph) {
    this.ipg = null;
    if (graph instanceof InstructionProgressGraph instructionProgressGraph) {
      this.ipg = instructionProgressGraph;
    }
    return super.load(graph);
  }

  @Override
  protected String label(Node node) {
    var label = new StringBuilder(super.label(node));

    if (ipg != null) {
      var context = ipg.getContext(node);
      if (withInstructions && context != null && !context.instructions().isEmpty()
          && !context.instructions().containsAll(ipg.instructions())) {
        var instructionsNotContained = ipg.instructions().stream()
            .filter(i -> !context.instructions().contains(i)).collect(Collectors.toSet());
        if (instructionsNotContained.size() < context.instructions().size()) {
          label.append("\\n!{")
              .append(instructionsNotContained.stream()
                  .map(Definition::simpleName).collect(Collectors.joining(", ")))
              .append("}");
        } else {
          label.append("\\n{")
              .append(context.instructions().stream()
                  .map(Definition::simpleName).collect(Collectors.joining(", ")))
              .append("}");
        }
      }
      if (withNameHints && context != null && !context.nameHints().isEmpty()) {
        label.append("\\n").append(String.join(", ", context.nameHints()));
      }
    }

    return label.toString();
  }
}
