// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import com.google.common.collect.Streams;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.iss.passes.tcg.lowering.nodes.TcgBr;
import vadl.iss.passes.tcg.lowering.nodes.TcgBrCond;
import vadl.iss.passes.tcg.lowering.nodes.TcgLabelNode;
import vadl.iss.passes.tcg.lowering.nodes.TcgSetLabel;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;

/**
 * An abstract class for performing data flow analysis over a control flow graph (CFG).
 * <p>
 * This class provides a framework for implementing different data flow analyses using the
 * kill/gen model. It supports both forward and backward analyses, as well as may and must analyses.
 * Subclasses should provide implementations for the abstract methods to define the specifics
 * of the analysis.
 * </p>
 *
 * @param <D> The type of data flow values (e.g., sets of variables).
 */
public abstract class DataFlowAnalysis<D> {

  /**
   * Creates a topological sorting of the CFG via depth-first-search. If a cycle is
   * detected, an error is thrown.
   */
  private class TopologicalSorting {

    /**
     * Marks all nodes that have been visited and put into {@link #ordering}.
     */
    protected Set<ControlNode> permanent = new HashSet<>();

    /**
     * Marks all nodes that have been visited and have <strong>not</strong> been put into
     * {@link #ordering}.
     */
    protected Set<ControlNode> temporary = new HashSet<>();

    /**
     * Contains the topologically ordered nodes.
     */
    public List<ControlNode> ordering = new ArrayList<>();

    public void sort(Graph cfg) {
      if (isForward()) {
        orderNode(getSingleNode(cfg, StartNode.class));
      } else {
        orderNode(getSingleNode(cfg, InstrEndNode.class));
      }
    }

    protected void orderNode(ControlNode node) {
      if (permanent.contains(node)) {
        // node has already been added to the ordering
        return;
      }
      node.ensureGraph().ensure(
          !temporary.contains(node),
          "Control flow cycle detected during acyclic data flow analysis"
      );
      temporary.add(node);
      for (var child : isForward() ? successorsOf(node) : predecessorsOf(node)) {
        orderNode(child);
      }
      permanent.add(node);
      ordering.addFirst(node);
    }
  }

  /**
   * Map storing the IN data flow value for each node.
   */
  protected Map<ControlNode, D> inValues = new HashMap<>();

  /**
   * Map storing the OUT data flow value for each node.
   */
  protected Map<ControlNode, D> outValues = new HashMap<>();

  /**
   * Performs the data flow analysis on the given control flow graph.
   *
   * @param cfg The control flow graph to analyze.
   */
  public void analyze(Graph cfg) {
    if (isAcyclic()) {
      analyzeAcyclic(cfg);
    } else {
      analyzeCyclic(cfg);
    }
  }

  /**
   * Performs the data flow analysis on the given control flow graph. Each node is visited once
   * in topological order.
   *
   * @param cfg The control flow graph to analyze.
   */
  private void analyzeAcyclic(Graph cfg) {
    var ordering = new TopologicalSorting();
    ordering.sort(cfg);
    ordering.ordering.forEach(this::visitNode);
  }

  /**
   * Performs the data flow analysis on the given (possibly cyclic) control flow graph.
   *
   * @param cfg The control flow graph to analyze.
   */
  private void analyzeCyclic(Graph cfg) {
    // FIXME: This algorithm currently does not terminate on cyclic graphs.
    //        It currently recomputes the in/out values even when the out/in values
    //        have not changed. To make it work we should add that check.
    // FIXME: Also, on larger cyclic graphs, this algorithm has an aggressively exponential runtime
    //        that can reach hours/days. It would be beneficial to use
    //        e.g. Tarjan's strongly connected components algorithm
    Queue<ControlNode> worklist = new ArrayDeque<>();

    // Initialize the IN and OUT sets for each node
    for (ControlNode node : cfg.getNodes(ControlNode.class).toList()) {
      inValues.put(node, initialFlow());
      outValues.put(node, initialFlow());
    }

    // Depending on the analysis direction, initialize the worklist
    if (isForward()) {
      worklist.add(getSingleNode(cfg, StartNode.class));
    } else {
      worklist.add(getSingleNode(cfg, InstrEndNode.class));
    }

    while (!worklist.isEmpty()) {
      ControlNode node = worklist.poll();
      // FIXME: as stated before, here we need to check whether in/out has changed
      visitNode(node);

      // Add neighbors to the worklist
      Set<ControlNode> neighbors = isForward() ? successorsOf(node) : predecessorsOf(node);
      worklist.addAll(neighbors);
    }
  }

  /**
   * Calls the {@link #transferFunction(ControlNode, Object)} on the given {@code node} to compute
   * the IN/OUT value.
   *
   * @param node The control node to compute the IN/OUT value for.
   */
  private void visitNode(ControlNode node) {
    D inValue;
    D outValue;

    if (isForward()) {
      // Forward analysis
      inValue = meetOfPredecessors(node);
      outValue = transferFunction(node, inValue);
    } else {
      // Backward analysis
      outValue = meetOfSuccessors(node);
      inValue = transferFunction(node, outValue);
    }

    inValues.put(node, inValue);
    outValues.put(node, outValue);
  }

  /**
   * Returns the initial data flow value.
   *
   * @return The initial data flow value.
   */
  protected abstract D initialFlow();

  /**
   * The meet operator for combining data flow values from multiple paths.
   * <p>
   * In a may analysis, the meet operator is typically set union.
   * In a must analysis, it is typically set intersection.
   * </p>
   *
   * @param values The collection of data flow values to meet.
   * @return The result of the meet operation.
   */
  protected abstract D meet(Set<D> values);

  /**
   * The transfer function for a node, applying the kill/gen effects.
   *
   * @param node  The node to apply the transfer function to.
   * @param input The input data flow value.
   * @return The output data flow value after applying the transfer function.
   */
  protected abstract D transferFunction(ControlNode node, D input);

  /**
   * Determines if the analysis is a forward analysis.
   *
   * @return {@code true} if the analysis is forward; {@code false} if backward.
   */
  protected abstract boolean isForward();

  /**
   * Determines if the analysis is a may analysis.
   *
   * @return {@code true} if the analysis is a may analysis; {@code false} if must.
   */
  protected abstract boolean isMayAnalysis();

  /**
   * Determines if the analysis can assume that the CGF is acyclic.
   *
   * @return {@code true} if the CFG is acyclic; {@code false} if it can be cyclic.
   */
  protected abstract boolean isAcyclic();

  /**
   * Checks if two data flow values are equal.
   * <p>
   * Subclasses may override this method if the data flow value type {@code D} does not
   * implement {@code equals} correctly.
   * </p>
   *
   * @param value1 The first data flow value.
   * @param value2 The second data flow value.
   * @return {@code true} if the values are equal; {@code false} otherwise.
   */
  protected boolean equals(D value1, D value2) {
    return value1.equals(value2);
  }

  /**
   * Computes the meet of the OUT values of the predecessors of a node.
   *
   * @param node The node whose predecessors are considered.
   * @return The result of the meet operation.
   */
  private D meetOfPredecessors(ControlNode node) {
    Set<D> values = new HashSet<>();
    for (ControlNode pred : predecessorsOf(node)) {
      values.add(outValues.get(pred));
    }
    return meet(values);
  }

  /**
   * Computes the meet of the IN values of the successors of a node.
   *
   * @param node The node whose successors are considered.
   * @return The result of the meet operation.
   */
  private D meetOfSuccessors(ControlNode node) {
    Set<D> values = new HashSet<>();
    for (ControlNode succ : successorsOf(node)) {
      values.add(inValues.get(succ));
    }
    if (node instanceof BranchEndNode endNode) {
      // the merge node of the branch end
      var mergeNode = endNode.usages().findFirst().map(MergeNode.class::cast).orElseThrow();
      values.add(inValues.get(mergeNode));
    }
    return meet(values);
  }

  /**
   * Retrieves the IN data flow value for a given node.
   *
   * @param node The node.
   * @return The IN data flow value.
   */
  public D getInValue(ControlNode node) {
    return requireNonNull(inValues.get(node));
  }

  /**
   * Retrieves the OUT data flow value for a given node.
   *
   * @param node The node.
   * @return The OUT data flow value.
   */
  public D getOutValue(ControlNode node) {
    return requireNonNull(outValues.get(node));
  }

  private Set<ControlNode> predecessorsOf(ControlNode node) {

    if (node instanceof TcgSetLabel setLabel) {
      // a label can have multiple predecessors: the direct predecessor and all jumps
      // to the label itself.
      var jumps = graphOf(setLabel).getNodes(Set.of(TcgBrCond.class, TcgBr.class))
          .filter(n -> ((TcgLabelNode) n).label().equals(setLabel.label()));
      var predecessor = Stream.of(requireNonNull(setLabel.predecessor()));
      return Streams.concat(jumps, predecessor)
          .map(ControlNode.class::cast)
          .collect(Collectors.toSet());
    }

    if (node instanceof MergeNode mergeNode) {
      // if the node is a merge node, we must look at its inputs (branch ends)
      return mergeNode.inputs().map(ControlNode.class::cast).collect(Collectors.toSet());
    }

    return node.predecessor() != null ? Set.of((ControlNode) node.predecessor()) : Set.of();
  }

  private Set<ControlNode> successorsOf(ControlNode node) {

    if (node instanceof TcgBr tcgBr) {
      // if the node is an unconditional branch, the only successor is the target node.
      return graphOf(node).getNodes(TcgSetLabel.class)
          .filter(n -> n.label().equals(tcgBr.label()))
          .collect(Collectors.toSet());
    }

    if (node instanceof TcgBrCond condBr) {
      // if the node is a conditional branch, the successors are the target label
      // and the successor.
      var targets = graphOf(node).getNodes(TcgSetLabel.class)
          .filter(n -> n.label().equals(condBr.label()));
      return Streams.concat(targets, condBr.successors())
          .map(ControlNode.class::cast)
          .collect(Collectors.toSet());
    }

    if (node instanceof BranchEndNode endNode) {
      // if the node is a branch node, we must find the corresponding merge node
      return endNode.usages().map(ControlNode.class::cast).collect(Collectors.toSet());
    }

    return node.successors()
        .map(ControlNode.class::cast)
        .collect(Collectors.toSet());
  }

  private Graph graphOf(ControlNode node) {
    return requireNonNull(node.graph());
  }
}
