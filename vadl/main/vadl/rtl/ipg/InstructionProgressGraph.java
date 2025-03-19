package vadl.rtl.ipg;

import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.rtl.ipg.nodes.SelectByInstructionNode;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

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

  private Collection<Instruction> addWithInstructions = Collections.emptyList();

  /**
   * Adds the given node with all its inputs to the graph.
   * If this or any input is a unique node, it will be only
   * added if there is no duplicate. Mark all added nodes
   * and its inputs with the given instructions.
   *
   * @param <T> node type
   * @param node node to add with its inputs
   * @param instructions instructions to mark nodes with
   * @return the node added to the graph or its duplicate
   */
  public <T extends Node> T addWithInputs(T node, Collection<Instruction> instructions) {
    addWithInstructions = instructions;
    var result = addWithInputs(node);
    addWithInstructions = Collections.emptyList();
    return result;
  }

  @Override
  public <T extends Node> T addWithInputs(T node) {
    var result = super.addWithInputs(node);
    if (result != node) {
      markNodeWithInputs(result, addWithInstructions);
    }
    return result;
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
    var result = add(node);
    markNode(node, instructions);
    return result;
  }

  @Override
  public <T extends Node> T add(T node) {
    contexts.computeIfAbsent(node, NodeContext::new);
    var result = super.add(node);
    if (!addWithInstructions.isEmpty()) {
      markNode(result, addWithInstructions);
    }
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
    if (instructions.isEmpty()) {
      return;
    }
    markNode(node, instructions);
    node.inputs().forEach(input -> markNodeWithInputs(input, instructions));
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
    Multimaps.index(nodes, Node::dataList).asMap().values()
        .forEach(candidates -> {
          // try to merge collection of candidate nodes that share the same data list
          // queue them based on the number of instructions they belong to (fewer first)
          var q = new ArrayDeque<T>();
          candidates.stream()
              .sorted(Comparator.comparing(
                  n -> getContext(n).instructions().size(), Comparator.reverseOrder()))
              .forEach(q::addLast);
          while (!q.isEmpty()) {
            var n1 = q.removeFirst();
            if (n1.isDeleted()) {
              continue;
            }
            // pick node to merge based on most equal inputs
            var n2opt = q.stream()
                .filter(n2 -> !n1.equals(n2) && !n2.isDeleted() && canMerge(n1, n2))
                .min(prioritizeEqualInputsComparator(n1));
            if (n2opt.isPresent()) {
              var n2 = n2opt.get();
              Streams.forEachPair(n1.inputs(), n2.inputs(), (i1, i2) -> {
                if (!i1.equals(i2)) {
                  ensure(i1 instanceof ExpressionNode && i2 instanceof ExpressionNode,
                      "Inputs must be an expression node");
                  var e1 = (ExpressionNode) i1;
                  var e2 = (ExpressionNode) i2;
                  // insert select-by-instruction nodes if necessary at inputs of n1
                  if (i1 instanceof SelectByInstructionNode sel1
                      && i2 instanceof SelectByInstructionNode sel2) {
                    sel1.merge(sel2);
                    getContext(sel1).instructions().addAll(getContext(sel2).instructions());
                  } else if (i1 instanceof SelectByInstructionNode sel1) {
                    for (Instruction instruction : getContext(n2).instructions()) {
                      sel1.add(instruction, e2);
                      getContext(sel1).instructions().add(instruction);
                    }
                  } else if (i2 instanceof SelectByInstructionNode sel2) {
                    for (Instruction instruction : getContext(n1).instructions()) {
                      sel2.add(instruction, e1);
                      getContext(sel2).instructions().add(instruction);
                    }
                    n1.replaceInput(i1, sel2);
                  } else {
                    var sel = add(new SelectByInstructionNode(e2.type()));
                    n1.replaceInput(i1, sel);
                    for (Instruction instruction : getContext(n1).instructions()) {
                      sel.add(instruction, e1);
                      getContext(sel).instructions().add(instruction);
                    }
                    for (Instruction instruction : getContext(n2).instructions()) {
                      sel.add(instruction, e2);
                      getContext(sel).instructions().add(instruction);
                    }
                    if (added != null) {
                      added.accept(sel);
                    }
                  }
                }
              });
              // update instruction sets in context for n1 and remove n2
              for (Instruction instruction : getContext(n2).instructions()) {
                getContext(n1).instructions().add(instruction);
              }
              n2.replaceAtAllUsages(n1);
              n2.safeDelete();
              if (removed != null) {
                removed.accept(n2);
              }
              q.addLast(n1);
            }
          }
        });
  }

  private boolean canMerge(Node n1, Node n2) {
    var s1 = getContext(n1).instructions();
    var s2 = getContext(n2).instructions();
    return s1.stream().noneMatch(s2::contains) && n1.inputs().count() == n2.inputs().count();
  }

  private int countEqualInputs(Node n1, Node n2) {
    var i1 = n1.inputs().toList();
    var i2 = n2.inputs().toList();
    if (i1.size() != i2.size()) {
      return 0;
    }
    var count = 0;
    for (int i = 0; i < i1.size(); i++) {
      if (i1.get(i) == i2.get(i)) {
        count++;
      }
    }
    return count;
  }

  private Comparator<Node> prioritizeEqualInputsComparator(Node ref) {
    return Comparator.comparing(n -> countEqualInputs(ref, n), Comparator.reverseOrder());
  }

  /**
   * Context for each node in the instruction progress graph keeping track of the instructions
   * the node belongs to.
   */
  public static class NodeContext {

    private final Node node;

    private final Set<Instruction> instructions;

    /**
     * Construct a node context for a node in the instruction progress graph.
     * Used in {@link InstructionProgressGraph#add}.
     *
     * @param node node
     */
    NodeContext(Node node) {
      this.node = node;
      this.instructions = new HashSet<>();
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
  }

}
