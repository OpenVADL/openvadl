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

package vadl.rtl.utils;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.rtl.ipg.nodes.SelectByInstructionNode;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;

/**
 * Utils for merging nodes in graphs by patching the inputs and replacing one node by the other.
 * This is used for merging non-concurrent reads/writes in the IPG generation.
 */
public class GraphMergeUtils {

  /**
   * Evaluate if we can merge two nodes using select nodes at their inputs. The nodes need to share
   * the same class, data list and their inputs need to be compatible
   * (see {@link GraphMergeUtils#canMergeInputs}).
   *
   * @param n1 node 1
   * @param n2 node 2 (would be replaced and deleted)
   * @return true if the nodes can be merged
   */
  public static boolean canMergeNodes(Node n1, Node n2) {
    return (n1 != n2 && n1.getClass().equals(n2.getClass())
        && n1.dataList().equals(n2.dataList())
        && canMergeInputs(n1, n2));
  }

  /**
   * Evaluate if we can merge the inputs of two nodes using select nodes.
   * This is: either they already are the same input or they are expression nodes with compatible
   * types.
   *
   * @param n1 node 1
   * @param n2 node 2 (would be replaced and deleted)
   * @return true if the node's inputs can be merged
   */
  public static boolean canMergeInputs(Node n1, Node n2) {
    if (n1.inputs().count() != n2.inputs().count()) {
      return false;
    }
    return Streams.zip(n1.inputs(), n2.inputs(), (i1, i2) -> {
      if (!(i1 instanceof ExpressionNode e1) || !(i2 instanceof ExpressionNode e2)) {
        return (i1 == i2); // can merge non-expression nodes only if they are already equal
      }
      return e1.isActiveIn(e2.ensureGraph())
          && mergeTypes(e1.type(), e2.type()) != null;
    })
    .allMatch(Boolean::booleanValue);
  }

  /**
   * Merge types. If types are equal, return it. If the types converted to a bit type are equal,
   * return the bit type. Null otherwise.
   *
   * @param t1 type 1
   * @param t2 type 2
   * @return merged type
   */
  public static @Nullable Type mergeTypes(Type t1, Type t2) {
    if (t1 == t2) {
      return t1;
    }
    if (t1 instanceof DataType d1 && t2 instanceof DataType d2) {
      var b1 = d1.toBitsType();
      var b2 = d2.toBitsType();
      if (b1.equals(b2)) {
        if (b1.bitWidth() == 1) {
          return Type.bool();
        }
        return b1;
      }
    }
    return null;
  }


  /**
   * Merge types. Select the type that the other one can be trivially cast to,
   * throws NullPointerException otherwise.
   *
   * @param t1 type 1
   * @param t2 type 2
   * @return type that can be trivially cast to the other one.
   */
  public static Type ensureMergeTypes(Type t1, Type t2) {
    return Objects.requireNonNull(mergeTypes(t1, t2));
  }

  /**
   * Count equal inputs of two nodes.
   *
   * @param n1 node 1
   * @param n2 node 2
   * @return number of equal inputs
   */
  public static long countEqualInputs(Node n1, Node n2) {
    return Streams.zip(n1.inputs(), n2.inputs(), Object::equals)
        .filter(Boolean::booleanValue).count();
  }

  /**
   * Node merge strategy. See {@link GraphMergeUtils#merge}.
   *
   * @param <T> type of node to merge
   */
  public interface MergeStrategy<T extends Node> {

    /**
     * Check if we can merge the given nodes.
     *
     * @param n1 node 1
     * @param n2 node 2 (would be replaced and deleted)
     * @return true if the nodes can be merged
     */
    boolean filter(T n1, T n2);

    /**
     * Comparator for sorting candidates for merging.
     *
     * @param pair1 pair 1
     * @param pair2 pair 2
     * @return a negative integer, zero, or a positive integer as the
     *     first argument is less than, equal to, or greater than the
     *     second.
     */
    int compare(Pair<T, T> pair1, Pair<T, T> pair2);

    /**
     * Merge the input i2 of n2 with the input i1 of n1.
     *
     * @param n1 node 1
     * @param i1 input of node 1
     * @param n2 node 2 (will be replaced and deleted)
     * @param i2 input of node 2
     * @return new input node
     */
    Node mergeInput(T n1, Node i1, T n2, Node i2);

    /**
     * Called before node 2 is replaced and deleted, but after merging inputs.
     *
     * @param n1 node 1
     * @param n2 node 2 (will be replaced and deleted)
     */
    default void beforeMerge(T n1, T n2) {
    }

    /**
     * Added a node to the graph as a new input.
     *
     * @param n1       node 1
     * @param i1       input of node 1
     * @param n2       node 2 (will be replaced and deleted)
     * @param i2       input of node 2
     * @param newInput new input node replacing input of node 1
     */
    default void added(T n1, Node i1, T n2, Node i2, Node newInput) {
    }
  }

  /**
   * Merge nodes from a collection using a strategy. Filter all pairs by a given filter, then sort
   * the candidates with the comparator of the strategy as a heuristic. The strategy is then called
   * to merge all inputs before replacing and deleting the second node with the first one.
   *
   * @param nodes    collection of nodes
   * @param strategy merge strategy
   * @param <T>      type of nodes to merge
   * @return list of merged nodes (in deleted state)
   */
  public static <T extends Node> List<T> merge(Collection<T> nodes, MergeStrategy<T> strategy) {
    var list = new ArrayList<>(nodes);
    var candidates = new ArrayList<Pair<T, T>>();
    var merged = new ArrayList<T>();

    // filter all pairs and sort
    for (int i = 0; i < list.size(); i++) {
      for (int j = i + 1; j < list.size(); j++) {
        var n1 = list.get(i);
        var n2 = list.get(j);
        if (strategy.filter(n1, n2)) {
          candidates.add(new Pair<>(n1, n2));
        }
      }
    }
    candidates.sort(strategy::compare);

    // merge as many pairs as possible
    for (Pair<T, T> candidate : candidates) {
      var n1 = candidate.left();
      var n2 = candidate.right();
      if (n1.isDeleted() || n2.isDeleted() || !strategy.filter(n1, n2)) {
        continue;
      }
      Streams.forEachPair(n1.inputs(), n2.inputs(), (i1, i2) -> {
        if (!i1.equals(i2)) {
          var newInput = strategy.mergeInput(n1, i1, n2, i2);
          var added = false;
          if (newInput.isUninitialized()) {
            newInput = n1.ensureGraph().add(newInput);
            added = true;
          }
          if (newInput != i1) {
            n1.replaceInput(i1, newInput);
          }
          if (added) {
            strategy.added(n1, i1, n2, i2, newInput);
          }
        }
      });
      strategy.beforeMerge(n1, n2);
      n2.replaceAndDelete(n1);
      merged.add(n2);
    }

    return merged;
  }

  /**
   * Merge strategy that introduces select nodes on a node's inputs.
   *
   * @param <T> type of nodes to merge
   */
  public static class SelectInputMergeStrategy<T extends Node> implements MergeStrategy<T> {

    private final Function<T, ExpressionNode> condition;

    /**
     * Construct new merge strategy using an extraction function for the condition.
     *
     * @param condition extraction function for condition from node
     */
    public SelectInputMergeStrategy(Function<T, ExpressionNode> condition) {
      this.condition = condition;
    }

    @Override
    public boolean filter(T n1, T n2) {
      return canMergeNodes(n1, n2);
    }

    @Override
    public int compare(Pair<T, T> pair1, Pair<T, T> pair2) {
      return -Long.compare(
          countEqualInputs(pair1.left(), pair1.right()),
          countEqualInputs(pair2.left(), pair2.right())
      );
    }

    @Override
    public Node mergeInput(T n1, Node i1, T n2, Node i2) {
      var e1 = (ExpressionNode) i1;
      var e2 = (ExpressionNode) i2;
      return new SelectNode(ensureMergeTypes(e2.type(), e1.type()),
          condition.apply(n2), e2, e1);
    }
  }

  /**
   * Merge strategy that introduces select-by-instruction nodes on a node's inputs.
   *
   * @param <T> type of nodes to merge
   */
  public static class SelectByInstructionInputMergeStrategy<T extends Node>
      implements MergeStrategy<T> {

    private final Function<T, Set<Instruction>> instructions;
    private final BiConsumer<Node, Node> merge;

    /**
     * Construct new merge strategy using an extraction function for the instruction set per node
     * and a callback to merge the instructions associated with two nodes on the first one.
     *
     * @param instructions extraction function for the instruction a node belongs to
     * @param merge        callback for merging the instructions on two nodes
     */
    public SelectByInstructionInputMergeStrategy(Function<T, Set<Instruction>> instructions,
                                                 BiConsumer<Node, Node> merge) {
      this.instructions = instructions;
      this.merge = merge;
    }

    @Override
    public boolean filter(T n1, T n2) {
      var ins1 = instructions.apply(n1);
      var ins2 = instructions.apply(n2);
      return canMergeNodes(n1, n2) && ins1.stream().noneMatch(ins2::contains);
    }

    @Override
    public int compare(Pair<T, T> pair1, Pair<T, T> pair2) {
      var inputs = -Long.compare(
          countEqualInputs(pair1.left(), pair1.right()),
          countEqualInputs(pair2.left(), pair2.right())
      );
      if (inputs == 0) {
        return Integer.compare(
            instructions.apply(pair1.left()).size() + instructions.apply(pair1.right()).size(),
            instructions.apply(pair2.left()).size() + instructions.apply(pair2.right()).size()
        );
      }
      return inputs;
    }

    @Override
    public Node mergeInput(T n1, Node i1, T n2, Node i2) {
      var e1 = (ExpressionNode) i1;
      var e2 = (ExpressionNode) i2;
      // insert select-by-instruction nodes if necessary at inputs of n1
      if (i1 instanceof SelectByInstructionNode sel1
          && i2 instanceof SelectByInstructionNode sel2) {
        sel1.merge(sel2);
        sel1.setType(ensureMergeTypes(e1.type(), e2.type()));
        merge.accept(sel1, n2);
        return i1;
      } else if (i1 instanceof SelectByInstructionNode sel1) {
        for (Instruction instruction : instructions.apply(n2)) {
          sel1.add(instruction, e2);
        }
        sel1.setType(ensureMergeTypes(e1.type(), e2.type()));
        merge.accept(sel1, n2);
        return i1;
      } else if (i2 instanceof SelectByInstructionNode sel2) {
        for (Instruction instruction : instructions.apply(n1)) {
          sel2.add(instruction, e1);
        }
        sel2.setType(ensureMergeTypes(e1.type(), e2.type()));
        merge.accept(sel2, n1);
        return i2;
      } else {
        var sel = new SelectByInstructionNode(ensureMergeTypes(e1.type(), e2.type()));
        for (Instruction instruction : instructions.apply(n1)) {
          sel.add(instruction, e1);
        }
        for (Instruction instruction : instructions.apply(n2)) {
          sel.add(instruction, e2);
        }
        return sel;
      }
    }

    @Override
    public void beforeMerge(T n1, T n2) {
      merge.accept(n1, n2);
    }

    @Override
    public void added(T n1, Node i1, T n2, Node i2, Node newInput) {
      merge.accept(newInput, n1);
      merge.accept(newInput, n2);
    }
  }

  /**
   * Merge strategy that merges select-by-instruction nodes. Does not need to modify inputs.
   */
  public static class SelectByInstructionMergeStrategy
      implements MergeStrategy<SelectByInstructionNode> {

    private final Function<SelectByInstructionNode, Set<Instruction>> instructions;
    private final BiConsumer<Node, Node> merge;

    /**
     * Construct new merge strategy using an extraction function for the instruction set per node
     * and a callback to merge the instructions associated with two nodes on the first one.
     *
     * @param instructions extraction function for the instruction a node belongs to
     * @param merge        callback for merging the instructions on two nodes
     */
    public SelectByInstructionMergeStrategy(Function<SelectByInstructionNode,
                                            Set<Instruction>> instructions,
                                            BiConsumer<Node, Node> merge) {
      this.instructions = instructions;
      this.merge = merge;
    }

    @Override
    public boolean filter(SelectByInstructionNode n1, SelectByInstructionNode n2) {
      var ins1 = instructions.apply(n1);
      var ins2 = instructions.apply(n2);
      return ins1.stream().noneMatch(ins2::contains);
    }

    @Override
    public int compare(Pair<SelectByInstructionNode, SelectByInstructionNode> pair1,
                       Pair<SelectByInstructionNode, SelectByInstructionNode> pair2) {
      // merge pairs with small number of instructions first
      return Integer.compare(
          instructions.apply(pair1.left()).size() + instructions.apply(pair1.right()).size(),
          instructions.apply(pair2.left()).size() + instructions.apply(pair2.right()).size()
      );
    }

    @Override
    public Node mergeInput(SelectByInstructionNode n1, Node i1,
                           SelectByInstructionNode n2, Node i2) {
      return i1; // do nothing, merge in beforeMerge()
    }

    @Override
    public void beforeMerge(SelectByInstructionNode n1, SelectByInstructionNode n2) {
      merge.accept(n1, n2);
      n1.merge(n2); // merge node here, merge algorithm does replaceAndDelete
    }

    @Override
    public void added(SelectByInstructionNode n1, Node i1,
                      SelectByInstructionNode n2, Node i2, Node newInput) {
      // not called, because mergeInput() does nothing
    }
  }

}
