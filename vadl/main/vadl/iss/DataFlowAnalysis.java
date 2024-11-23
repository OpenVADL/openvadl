package vadl.iss;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.Condition;
import java.util.stream.Collectors;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ScheduledNode;
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
    Queue<ControlNode> worklist = new LinkedList<>();

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

      D inValue, outValue;
      D oldInValue = requireNonNull(inValues.get(node));
      D oldOutValue = requireNonNull(outValues.get(node));

      if (isForward()) {
        // Forward analysis
        inValue = meetOfPredecessors(node, cfg);
        outValue = transferFunction(node, inValue);
      } else {
        // Backward analysis
        outValue = meetOfSuccessors(node, cfg);
        inValue = transferFunction(node, outValue);
      }

      inValues.put(node, inValue);
      outValues.put(node, outValue);

      // Add neighbors to the worklist
      Set<ControlNode> neighbors = isForward() ? successorsOf(node) : predecessorsOf(node);
      worklist.addAll(neighbors);

    }
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
   * @param cfg  The control flow graph.
   * @return The result of the meet operation.
   */
  private D meetOfPredecessors(ControlNode node, Graph cfg) {
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
   * @param cfg  The control flow graph.
   * @return The result of the meet operation.
   */
  private D meetOfSuccessors(ControlNode node, Graph cfg) {
    Set<D> values = new HashSet<>();
    for (ControlNode succ : successorsOf(node)) {
      values.add(inValues.get(succ));
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
    return node instanceof MergeNode mergeNode
        ? mergeNode.inputs().map(ControlNode.class::cast).collect(Collectors.toSet())
        : node.predecessor() != null ? Set.of((ControlNode) node.predecessor()) : Set.of();
  }

  private Set<ControlNode> successorsOf(ControlNode node) {
    return node.successors()
        .map(ControlNode.class::cast)
        .collect(Collectors.toSet());
  }
}
