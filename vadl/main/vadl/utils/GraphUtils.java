package vadl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Register;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * A collection of useful utility methods on graphs.
 */
public class GraphUtils {


  /**
   * Searches for a node of the nodeClass and ensures that there is exactly one such
   * node.
   * The found node is returned.
   *
   * @param graph     The graph to search in.
   * @param nodeClass The node types class.
   * @param <T>       The type of node.
   * @return The found node of type T.
   */
  public static <T extends Node> T getSingleNode(Graph graph, Class<T> nodeClass) {
    var nodes = graph.getNodes(nodeClass).toList();
    graph.ensure(nodes.size() == 1, "Expected one node of type %s but found %s",
        nodeClass.getName(), nodes.size());
    return nodes.get(0);
  }

  /**
   * Find and ensures a single leaf node of the given node class with the given node
   * as search root.
   * If it could not find exactly one leaf or the leaf is not an instance of the given
   * node class, this method will fail.
   *
   * @param node      the search root
   * @param nodeClass the expected node class of the leaf
   * @return the found leaf node
   */
  public static <T extends Node> T getSingleLeafNode(Node node, Class<T> nodeClass) {
    var leaves = getLeafNodes(node).toList();
    node.ensure(leaves.size() == 1, "Expected exactly 1 leave node, but got %s", leaves.size());
    var result = leaves.get(0);
    result.ensure(nodeClass.isInstance(result), "Expected to be of type %s", nodeClass.getName());
    return nodeClass.cast(result);
  }

  /**
   * Finds all leaf nodes (nodes that have no inputs) with the given node as search root.
   */
  public static Stream<Node> getLeafNodes(Node node) {
    var inputs = node.inputs().toList();
    if (inputs.isEmpty()) {
      return Stream.of(node);
    }
    return inputs.stream()
        .flatMap(GraphUtils::getLeafNodes);
  }


  //// GRAPH CREATION UTILS ////

  public static BuiltInCall binaryOp(BuiltInTable.BuiltIn op, Constant.Value a, Constant.Value b) {
    return new BuiltInCall(
        op,
        new NodeList<>(
            new ConstantNode(a),
            new ConstantNode(b)
        ),
        a.type()
    );
  }

  public static BuiltInCall binaryOp(BuiltInTable.BuiltIn op, Type resultType, ExpressionNode a,
                                     ExpressionNode b) {
    return new BuiltInCall(
        op,
        new NodeList<>(
            a,
            b
        ),
        resultType
    );
  }

  public static TypeCastNode cast(ExpressionNode val, Type type) {
    return new TypeCastNode(val, type);
  }

  public static TypeCastNode cast(Type type, ExpressionNode val) {
    return new TypeCastNode(val, type);
  }

  public static ConstantNode intSNode(long val, int width) {
    return new ConstantNode(intS(val, width));
  }

  public static ConstantNode intUNode(long val, int width) {
    return new ConstantNode(intU(val, width));
  }

  public static ConstantNode bitsNode(long val, int width) {
    return new ConstantNode(bits(val, width));
  }

  public static ReadRegNode readReg(Register register,
                                    @Nullable Counter.RegisterCounter staticCounterAddress
  ) {
    return new ReadRegNode(register, register.resultType(), staticCounterAddress);
  }

  public static WriteRegNode writeReg(Register register,
                                      ExpressionNode val,
                                      @Nullable Counter.RegisterCounter staticCounterAddress) {
    return new WriteRegNode(register, val, staticCounterAddress);
  }

  //// CONSTANT VALUE CONSTRUCTION ////

  public static Constant.Value intS(long val, int width) {
    return Constant.Value.of(val, Type.signedInt(width));
  }

  public static Constant.Value intU(long val, int width) {
    return Constant.Value.of(val, Type.unsignedInt(width));
  }

  public static Constant.Value bits(long val, int width) {
    return Constant.Value.of(val, Type.bits(width));
  }


  public static Constant.Value bool(boolean val) {
    return Constant.Value.of(val);
  }

  public static Constant.Tuple.Status status(boolean negative, boolean zero, boolean carry,
                                             boolean overflow) {
    return new Constant.Tuple.Status(negative, zero, carry, overflow);
  }

}
