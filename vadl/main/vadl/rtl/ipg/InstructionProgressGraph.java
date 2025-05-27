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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import vadl.rtl.utils.GraphMergeUtils;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * The instruction progress graph is used to combine the behavior of all instructions into
 * a single graph.
 *
 * <p>The graph keeps track of a context for each node, which holds the set of instructions the
 * node occurs in.
 */
public class InstructionProgressGraph extends Graph {

  private final IdentityHashMap<Node, NodeContext> contexts = new IdentityHashMap<>();
  private final Set<Instruction> instructions = new HashSet<>();

  /**
   * Constructs a new instruction progress graph.
   *
   * @param name name
   * @param parentDefinition parent definition, usually the ISA definition
   */
  public InstructionProgressGraph(String name, Definition parentDefinition) {
    super(name, parentDefinition);
  }

  /**
   * Get all instructions present in the instruction progress graph.
   *
   * @return set of all instructions
   */
  public Set<Instruction> instructions() {
    return instructions;
  }

  /**
   * Adds the given node with all its inputs to the graph.
   * If this or any input is a unique node, it will be only
   * added if there is no duplicate. Mark all added nodes
   * and its inputs with the given instructions.
   * <i>This does not mark all input nodes of the node
   * referenced by passing already active nodes.</i>
   *
   * @param <T> node type
   * @param node node to add with its inputs
   * @param instructions instructions to mark nodes with
   * @return the node added to the graph or its duplicate
   */
  public <T extends Node> T addWithInputs(T node, Collection<Instruction> instructions) {
    var ignoreSet = new HashSet<Node>();
    collectActiveInputs(node, ignoreSet);
    var result = addWithInputs(node);
    markNodeWithInputs(result, instructions, ignoreSet);
    return result;
  }

  private void collectActiveInputs(Node node, Set<Node> set) {
    if (node.isActive()) {
      set.add(node);
    } else {
      node.inputs().forEach(input -> collectActiveInputs(input, set));
    }
  }

  /**
   * Adds the node to the graph (without its inputs).
   * If the node is a unique node, it will check for duplication int the graph.
   * Mark the node with the given instructions.
   *
   * @param <T> node type
   * @param node to be added
   * @param instructions instructions to mark node with
   * @return the node added to the graph or its duplicate
   */
  public <T extends Node> T add(T node, Collection<Instruction> instructions) {
    node = add(node);
    markNode(node, instructions);
    return node;
  }

  @Override
  public <T extends Node> T add(T node) {
    var result = super.add(node);
    contexts.computeIfAbsent(result, NodeContext::new);
    return result;
  }

  /**
   * Replace node with replacements while merging marked instructions on the replacement node.
   *
   * @param <T> node type
   * @param node node to be replaced
   * @param replacement the node to replace node with
   * @return the replacement node
   */
  public <T extends Node> T replace(T node, T replacement) {
    var instructions = getContext(node).instructions();
    replacement = node.replace(replacement);
    markNodeWithInputs(replacement, instructions);
    return replacement;
  }

  /**
   * Replaces this node by the given node while merging marked instructions on the replacement node.
   * After node is replaced, it is removed from the graph.
   *
   * @param node node to be replaced
   * @param replacement node that replaces node
   * @return the replacement node
   */
  public <T extends Node> T replaceAndDelete(T node, T replacement) {
    var instructions = getContext(node).instructions();
    replacement = node.replaceAndDelete(replacement);
    markNodeWithInputs(replacement, instructions);
    return replacement;
  }

  @Override
  protected void remove(Node node) {
    super.remove(node);
    contexts.remove(node);
  }

  /**
   * Get the context object for a given node. The node must be added to this instruction progress
   * graph instance.
   *
   * @param node graph node to get the context for
   * @return context
   */
  public NodeContext getContext(Node node) {
    ensure(node.graph() == this, "Node %s to mark is not part of %s",
        node, this);
    var context = contexts.get(node);
    ensure(context != null, "Node %s is missing a context in %s", node, this);
    return Objects.requireNonNull(context);
  }

  /**
   * Add instructions to the set in a node's context.
   * Used by {@link InstructionProgressGraph#markNodeWithInputs}.
   *
   * @param node node to add the instruction to
   * @param instructions instructions
   */
  public void markNode(Node node, Collection<Instruction> instructions) {
    getContext(node).instructions().addAll(instructions);
    this.instructions.addAll(instructions);
  }

  /**
   * Add instructions to the set in a node's context and the contexts of all its inputs
   * recursively. This is used during creation of the instruction progress graph.
   *
   * @param node node to add the instruction to
   * @param instructions instructions
   */
  public void markNodeWithInputs(Node node, Collection<Instruction> instructions) {
    markNodeWithInputs(node, instructions, Collections.emptySet());
  }

  private void markNodeWithInputs(Node node, Collection<Instruction> instructions,
                                  Set<Node> ignoreSet) {
    if (ignoreSet.contains(node) || instructions.isEmpty()) {
      return;
    }
    markNode(node, instructions);
    node.inputs().forEach(input -> markNodeWithInputs(input, instructions, ignoreSet));
  }

  /**
   * Merge nodes of a type if the instructions they belong to do not overlap. The inputs of the
   * merged nodes are modified by adding select-by-instruction nodes.
   *
   * @param nodes set of nodes to consider
   * @param removed handles removed nodes
   * @param added handles added nodes
   * @param <T> type of nodes to merge
   */
  public <T extends Node> void merge(Set<T> nodes, @Nullable Consumer<T> removed,
                                     @Nullable Consumer<Node> added) {
    var merged = GraphMergeUtils.merge(nodes,
        new GraphMergeUtils.SelectByInstructionInputMergeStrategy<>(
            node -> getContext(node).instructions(),
            (n1, n2) -> getContext(n1).instructions().addAll(getContext(n2).instructions())
        ) {
          @Override
          public void added(T n1, Node i1, T n2, Node i2, Node newInput) {
            super.added(n1, i1, n2, i2, newInput);
            if (added != null) {
              added.accept(newInput);
            }
          }
        }
    );
    if (removed != null) {
      merged.forEach(removed);
    }
  }

  /**
   * Context for each node in the instruction progress graph keeping track of the instructions
   * the node belongs to.
   */
  public static class NodeContext {

    private final Node node;

    private final Set<Instruction> instructions;

    private final Set<String> nameHints;

    /**
     * Construct a node context for a node in the instruction progress graph.
     * Used in {@link InstructionProgressGraph#add}.
     *
     * @param node node
     */
    NodeContext(Node node) {
      this.node = node;
      this.instructions = new HashSet<>();
      this.nameHints = new HashSet<>();
    }

    /**
     * Get the context's node.
     *
     * @return node
     */
    public Node node() {
      return node;
    }

    /**
     * Get the set of instructions the node belongs to.
     *
     * @return set of instructions
     */
    public Set<Instruction> instructions() {
      return instructions;
    }

    /**
     * Get name hints for generating variable names.
     *
     * @return set of name hints
     */
    public Set<String> nameHints() {
      return nameHints;
    }

    /**
     * Get the shortest name hint in terms of string length.
     *
     * @return shortest name hint
     */
    public Optional<String> shortestNameHint() {
      return nameHints.stream()
          .min(Comparator.comparing(String::length));
    }

    /**
     * Get the shortest name hint in terms of string length.
     *
     * @param existing existing names to not consider
     * @return shortest name hint
     */
    public Optional<String> shortestNameHint(Set<String> existing, int maxLength) {
      return nameHints.stream()
          .filter(name -> !existing.contains(name))
          .map(name -> StringUtils.truncate(name, maxLength))
          .map(name -> StringUtils.stripEnd(name, "_"))
          .min(Comparator.comparing(String::length));
    }

  }

}
