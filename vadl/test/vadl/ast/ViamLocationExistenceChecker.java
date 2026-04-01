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

package vadl.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.utils.SourceLocation;
import vadl.viam.DefProp;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

public class ViamLocationExistenceChecker {

  /**
   * Verifies that all nodes in the specification's VIAM graphs have valid source locations.
   *
   * @param spec the specification to verify
   * @throws IllegalStateException if any node lacks a valid source location
   */
  public static void verify(Specification spec) {
    var visitor = new LocationCheckingVisitor();
    spec.accept(visitor);
    visitor.throwIfNodesWithoutLocation();
  }

  private static class LocationCheckingVisitor extends DefinitionVisitor.Recursive {

    private final List<Node> nodesWithoutLocation = new ArrayList<>();

    @Override
    public void beforeTraversal(Definition definition) {
      // Check if the definition has behaviors (graphs)
      if (definition instanceof DefProp.WithBehavior withBehavior) {
        for (var graph : withBehavior.behaviors()) {
          if (graph != null) {
            verifyGraph(graph);
          }
        }
      }
    }

    private void verifyGraph(Graph graph) {
      Set<Node> visited = new HashSet<>();
      graph.getNodes().forEach(node -> verifyNode(node, visited));
    }

    private void verifyNode(Node node, Set<Node> visited) {
      // Avoid infinite loops by tracking visited nodes
      if (!visited.add(node)) {
        return;
      }

      SourceLocation location = node.location();
      if (location == null || location.equals(SourceLocation.INVALID_SOURCE_LOCATION)) {
        nodesWithoutLocation.add(node);
      }

      // Recursively check all input nodes
      node.inputs().forEach(input -> verifyNode(input, visited));

      // Recursively check all successor nodes
      node.successors().forEach(succ -> verifyNode(succ, visited));
    }

    private void throwIfNodesWithoutLocation() {
      if (!nodesWithoutLocation.isEmpty()) {
        var sb = new StringBuilder();
        sb.append("Found ").append(nodesWithoutLocation.size())
            .append(" node(s) without valid source location:\n");

        for (Node node : nodesWithoutLocation) {
          sb.append("  - ").append(node)
              .append(" (type: ").append(node.getClass().getSimpleName())
              .append(")\n");
        }

        throw new IllegalStateException(sb.toString());
      }
    }
  }
}
