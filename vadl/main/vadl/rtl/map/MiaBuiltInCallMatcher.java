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
import vadl.rtl.ipg.nodes.RtlInstructionWordSliceNode;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Implements a collection of matchers for the different types of MiA builtin calls that can map
 * parts of the instruction progress graph. E.g., for an instruction write builtin node we match
 * all write nodes in the instruction progress graph.
 */
public class MiaBuiltInCallMatcher {

  interface Matcher {
    boolean match(Node matchNode, MiaBuiltInCall mapNode, Set<Node> doneNodes);
  }

  private static final IdentityHashMap<BuiltInTable.BuiltIn, Matcher> MATCHERS =
      new IdentityHashMap<>();

  static {
    MATCHERS.put(BuiltInTable.DECODE, (matchNode, mapNode, doneNodes) -> {
      if (matchNode instanceof ConstantNode) {
        return false;
      }
      return resolveDecode(matchNode);
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_READ, (matchNode, mapNode, doneNodes) -> {
      return matchNode instanceof ReadResourceNode n
          && mapNode.matchResource(n.resourceDefinition());
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_WRITE, (matchNode, mapNode, doneNodes) -> {
      return matchNode instanceof WriteResourceNode n
          && mapNode.matchResource(n.resourceDefinition());
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_COMPUTE, (matchNode, mapNode, doneNodes) -> {
      if (matchNode instanceof ConstantNode) {
        return false;
      }
      return resolveCompute(matchNode, doneNodes);
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_ADDRESS, (matchNode, mapNode, doneNodes) -> {
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
    MATCHERS.put(BuiltInTable.INSTRUCTION_RESULTS, (matchNode, mapNode, doneNodes) -> {
      return matchNode.usages().anyMatch(use -> {
        if (use instanceof WriteResourceNode n && mapNode.matchResource(n.resourceDefinition())) {
          return (n.hasAddress() && n.value() == matchNode
              && resolveDoneThroughUnaryNodes(matchNode, doneNodes));
        }
        return false;
      });
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_READ_OR_FORWARD, (matchNode, mapNode, doneNodes) -> {
      return matchNode instanceof ReadResourceNode n
          && mapNode.matchResource(n.resourceDefinition());
    });
    MATCHERS.put(BuiltInTable.INSTRUCTION_VERIFY, (matchNode, mapNode, doneNodes) -> {
      return matchNode.usages().anyMatch(use ->
          (use instanceof WriteRegTensorNode writeRegNode && writeRegNode.isPcAccess()));
    });
  }

  /**
   * Resolve if a node's value can be determined in the decode mapping. This is, if none of its
   * inputs are based on a read node, and it is not a write node.
   *
   * @param node node to check
   * @return true, if part of decode mapping
   */
  private static boolean resolveDecode(Node node) {
    if (node instanceof ReadResourceNode || node instanceof WriteResourceNode) {
      return false;
    }
    if (node instanceof RtlInstructionWordSliceNode) {
      return true;
    }
    return node.inputs().allMatch(MiaBuiltInCallMatcher::resolveDecode);
  }

  /**
   * Resolve if a done node is reachable through a chain of unary nodes. The unary nodes we have
   * don't include calculation, just truncate and sign/zero-extend nodes. Used for determine if
   * a result can be mapped for {@link BuiltInTable#INSTRUCTION_RESULTS} nodes.
   *
   * @param node node to resolve chain of unary nodes for
   * @param doneNodes set of already mapped nodes
   * @return true, if done node is reachable through chain of unary nodes
   */
  private static boolean resolveDoneThroughUnaryNodes(Node node, Set<Node> doneNodes) {
    if (doneNodes.contains(node)) {
      return true;
    }
    if (node instanceof UnaryNode unaryNode) {
      return resolveDoneThroughUnaryNodes(unaryNode.value(), doneNodes);
    }
    return false;
  }

  private static boolean isCompute(Node matchNode) {
    if (matchNode instanceof BuiltInCall n) {
      // TODO fix meaning of compute
      return n.arguments().stream()
          .anyMatch(i -> (i.type() instanceof DataType dt && dt.bitWidth() > 1));
    }
    return false;
  }

  /**
   * Resolve if the given node should be mapped as part of the compute built-in.
   *
   * @param matchNode node to be mapped
   * @param doneNodes set of already mapped nodes
   * @return true, if node is part of compute
   */
  private static boolean resolveCompute(Node matchNode, Set<Node> doneNodes) {
    if (matchNode instanceof BuiltInCall n) {
      if (isCompute(matchNode)) {
        return true;
      }
      return matchNode.inputs()
          .allMatch(input -> doneNodes.contains(input) || resolveCompute(input, doneNodes));
    }
    if (matchNode instanceof ConstantNode) {
      return true;
    }
    if (matchNode instanceof SelectNode || matchNode instanceof UnaryNode) {
      return matchNode.inputs()
          .allMatch(input -> doneNodes.contains(input) || resolveCompute(input, doneNodes));
    }
    return false;
  }

  /**
   * Filter a set of nodes for the nodes that a MiA builtin call refers to, e.g., for an instruction
   * write builtin node we match all write nodes.
   *
   * @param mapNode MiA builtin call to filter by
   * @param nodes set of nodes to filter
   * @return filtered set of nodes
   */
  public Set<Node> match(MiaBuiltInCall mapNode, Set<Node> nodes, Set<Node> doneNodes) {
    var matcher = MATCHERS.get(mapNode.builtIn());
    if (matcher == null) {
      return Collections.emptySet();
    }
    return nodes.stream()
        .filter(matchNode -> matcher.match(matchNode, mapNode, doneNodes))
        .collect(Collectors.toSet());
  }

}
