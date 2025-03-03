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

package vadl.vdt.target.common;

import java.util.Objects;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.target.common.dto.DecisionTreeStatistics;

/**
 * Calculate general statistics about the structure of a decision tree, such as the number of nodes,
 * the number of leaf nodes, the maximum depth, the minimum depth, and the average depth.
 */
public class DecisionTreeStatsCalculator implements Visitor<DecisionTreeStatistics> {

  public static DecisionTreeStatistics statistics(Node node) {
    return new DecisionTreeStatsCalculator().calculate(node);
  }

  public DecisionTreeStatistics calculate(Node node) {
    return Objects.requireNonNull(node.accept(this));
  }

  @Override
  public DecisionTreeStatistics visit(InnerNode node) {

    var stats = new DecisionTreeStatistics();

    stats.setNumberOfNodes(1);
    stats.setNumberOfLeafNodes(0);

    stats.setMaxDepth(0);
    stats.setMinDepth(Integer.MAX_VALUE);
    stats.setAvgDepth(0);

    for (Node child : node.children()) {
      DecisionTreeStatistics childStats = Objects.requireNonNull(child.accept(this));

      stats.setNumberOfNodes(stats.getNumberOfNodes() + childStats.getNumberOfNodes());
      stats.setNumberOfLeafNodes(stats.getNumberOfLeafNodes() + childStats.getNumberOfLeafNodes());
      stats.setMaxDepth(Math.max(stats.getMaxDepth(), childStats.getMaxDepth()));
      stats.setMinDepth(Math.min(stats.getMinDepth(), childStats.getMinDepth()));
      stats.setMaxInstructionWidth(
          Math.max(stats.getMaxInstructionWidth(), childStats.getMaxInstructionWidth()));

      double avgDepth = (childStats.getAvgDepth() + 1) * childStats.getNumberOfLeafNodes();
      stats.setAvgDepth(stats.getAvgDepth() + avgDepth);
    }

    stats.setMinDepth(stats.getMinDepth() + 1);
    stats.setMaxDepth(stats.getMaxDepth() + 1);
    stats.setAvgDepth(stats.getAvgDepth() / stats.getNumberOfLeafNodes());

    return stats;
  }

  @Override
  public DecisionTreeStatistics visit(LeafNode node) {
    var stats = new DecisionTreeStatistics();
    stats.setNumberOfNodes(1);
    stats.setNumberOfLeafNodes(1);
    stats.setMaxDepth(0);
    stats.setMinDepth(0);
    stats.setAvgDepth(0);
    stats.setMaxInstructionWidth(node.instruction().width());
    return stats;
  }
}
