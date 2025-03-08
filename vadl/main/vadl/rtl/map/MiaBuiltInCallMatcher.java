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

package vadl.rtl.map;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Implements a collection of matchers for the different types of MiA builtin calls that can map
 * parts of the instruction progress graph. E.g., for an instruction write builtin node we match
 * all write nodes in the instruction progress graph.
 */
public class MiaBuiltInCallMatcher {

  interface Matcher {
    boolean match(Node matchNode, MiaBuiltInCall mapNode);
  }

  private static final IdentityHashMap<BuiltInTable.BuiltIn, Matcher> MATCHERS =
      new IdentityHashMap<>();

  static {
    MATCHERS.put(BuiltInTable.INSTRUCTION_READ, (matchNode, mapNode) -> {
      return matchNode instanceof ReadResourceNode n
          && mapNode.matchResource(n.resourceDefinition());
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_WRITE, (matchNode, mapNode) -> {
      return matchNode instanceof WriteResourceNode n
          && mapNode.matchResource(n.resourceDefinition());
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_COMPUTE, (matchNode, mapNode) -> {
      if (matchNode instanceof BuiltInCall n) {
        // TODO fix meaning of compute
        return n.arguments().stream()
            .anyMatch(i -> (i.type() instanceof DataType dt && dt.bitWidth() > 1));
      }
      return false;

    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_ADDRESS, (matchNode, mapNode) -> {
      return matchNode.usages().anyMatch(use -> {
        if (use instanceof ReadResourceNode n && mapNode.matchResource(n.resourceDefinition())) {
          return (n.hasAddress() && n.address() == matchNode);
        }
        if (use instanceof WriteResourceNode n && mapNode.matchResource(n.resourceDefinition())) {
          return (n.hasAddress() && n.address() == matchNode);
        }
        return false;
      });
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_RESULTS, (matchNode, mapNode) -> {
      return matchNode.usages().anyMatch(use -> {
        if (use instanceof WriteResourceNode n && mapNode.matchResource(n.resourceDefinition())) {
          return (n.hasAddress() && n.value() == matchNode);
        }
        return false;
      });
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_READ_OR_FORWARD, (matchNode, mapNode) -> {
      return matchNode instanceof ReadResourceNode n
          && mapNode.matchResource(n.resourceDefinition());
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_VERIFY, (matchNode, mapNode) -> {
      return false; // TODO
    });
  }

  /**
   * Filter a set of nodes for the nodes that a MiA builtin call refers to, e.g., for an instruction
   * write builtin node we match all write nodes.
   *
   * @param mapNode MiA builtin call to filter by
   * @param nodes set of nodes to filter
   * @return filtered set of nodes
   */
  public Set<Node> match(MiaBuiltInCall mapNode, Set<Node> nodes) {
    var matcher = MATCHERS.get(mapNode.builtIn());
    if (matcher == null) {
      return Collections.emptySet();
    }
    return nodes.stream()
        .filter(matchNode -> matcher.match(matchNode, mapNode))
        .collect(Collectors.toSet());
  }

}
