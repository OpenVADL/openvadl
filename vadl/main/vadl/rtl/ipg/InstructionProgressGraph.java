package vadl.rtl.ipg;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
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

  @Override
  public <T extends Node> T add(T node) {
    contexts.put(node, new NodeContext(node));
    return super.add(node);
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
   * Add an instruction to the set in a node's context.
   * Used by {@link InstructionProgressGraph#markNodeWithInputs}.
   *
   * @param node node to add the instruction to
   * @param instruction instruction
   */
  public void markNode(Node node, Instruction instruction) {
    getContext(node).instructions().add(instruction);
    instructions.add(instruction);
  }

  /**
   * Add an instruction to the set in a node's context and the contexts of all its inputs
   * recursively. This is used during creation of the instruction progress graph.
   *
   * @param node node to add the instruction to
   * @param instruction instruction
   */
  public void markNodeWithInputs(Node node, Instruction instruction) {
    markNode(node, instruction);
    node.inputs().forEach(input -> markNodeWithInputs(input, instruction));
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
