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

package vadl.utils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * A collection of useful utility methods on graphs.
 */
public class GraphUtils {

  /**
   * Searches for a node of the nodeClass.
   * The found node is returned.
   *
   * @param graph     The graph to search in.
   * @param nodeClass The node types class.
   * @param <T>       The type of node.
   * @return The found nodes of type T.
   */
  public static <T extends Node> List<T> getNodes(Graph graph, Class<T> nodeClass) {
    return graph.getNodes(nodeClass).toList();
  }

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

  /**
   * Recursively finds all input nodes that by a given filter.
   */
  public static Stream<Node> getInputNodes(Node node,
                                           Function<DependencyNode, Boolean> filter) {
    return node.inputs()
        .flatMap(i -> {
          if (filter.apply((DependencyNode) i)) {
            return Stream.concat(Stream.of(i), getInputNodes(i, filter));
          }
          return getInputNodes(i, filter);
        });
  }

  /**
   * Retrieve all usages of the given node by unrolling all let nodes.
   * If a usage node is a let node, all usages of the let node are recursively added as well.
   */
  public static Stream<Node> getUsagesByUnrollingLets(ExpressionNode node) {
    return node.usages()
        .flatMap(u -> {
          if (u instanceof LetNode letNode) {
            return getUsagesByUnrollingLets(letNode);
          }
          return Stream.of(u);
        });
  }

  /**
   * Determines if a specified filter applies to the given node or at least one of its dependencies.
   *
   * @param node   The starting node to check.
   * @param filter A function that applies a condition to each node,
   *               returning true if the node has a dependency and should be considered.
   * @return true if itself or any dependency satisfies the filter condition; false otherwise.
   */
  public static boolean isOrhasDependencies(Node node, Function<Node, Boolean> filter) {
    return filter.apply((Node) node) || hasDependencies(node, filter);
  }


  /**
   * Determines if a given node has dependencies based on a provided filter function.
   *
   * @param node   The starting node to check for dependencies.
   * @param filter A function that applies a condition to each node,
   *               returning true if the node has a dependency and should be considered.
   * @return true if any dependency satisfies the filter condition; false otherwise.
   */
  public static boolean hasDependencies(Node node, Function<Node, Boolean> filter) {
    GraphVisitor<Boolean> visitor = new GraphVisitor<>() {
      @Override
      public @Nonnull Boolean visit(Node from, @Nullable Node to) {
        if (to == null || filter.apply(to)) {
          return filter.apply(to);
        }
        return to.inputs().anyMatch(input -> visit(node, input));
      }
    };

    return node.inputs().anyMatch(input -> visitor.visit(node, input));
  }

  /**
   * Determines if a given node has users based on a provided filter function.
   *
   * @param node   The starting node to check for users.
   * @param filter A function that applies a condition to each node, returning true if the node
   *               is considered a user and satisfies the condition.
   * @return true if any user satisfies the filter condition; false otherwise.
   */
  public static boolean hasUser(Node node, Function<Node, Boolean> filter) {
    GraphVisitor<Boolean> visitor = new GraphVisitor<>() {
      @Override
      public @Nonnull Boolean visit(Node from, @Nullable Node to) {
        if (to == null || filter.apply(to)) {
          return filter.apply(to);
        }
        return to.usages().anyMatch(input -> visit(node, input));
      }
    };

    return node.usages().anyMatch(input -> visitor.visit(node, input));
  }

  /**
   * Retrieves a single instance of a specified type of user node from the provided node's usages.
   * The method ensures that there is exactly one user of the specified type.
   *
   * @param node      The node whose usages are to be inspected.
   * @param userClass The class type of the user node to retrieve.
   * @param <T>       The type of the user node.
   * @return The single user node of the specified type.
   */
  public static <T extends Node> T getSingleUsage(Node node, Class<T> userClass) {
    var users = node.usages().filter(userClass::isInstance).toList();
    node.ensure(users.size() == 1, "Expected exactly one user of type %s but got %s",
        userClass.getSimpleName(), users.size());
    //noinspection unchecked
    return (T) users.get(0);
  }

  /// / GRAPH CREATION UTILS ////

  /**
   * Creates a unary operation built-in call with the specified operation and a constant value.
   *
   * @param op The built-in operation to be performed.
   * @param a  The constant value operand.
   * @return A new instance of {@code BuiltInCall} representing the unary operation.
   */
  public static BuiltInCall unaryOp(BuiltInTable.BuiltIn op, Constant.Value a) {
    return new BuiltInCall(
        op,
        new NodeList<>(
            new ConstantNode(a)
        ),
        a.type()
    );
  }

  /**
   * Creates a binary operation built-in call with the specified operation and constant values.
   *
   * @param op The built-in operation to be performed.
   * @param a  The first constant value operand.
   * @param b  The second constant value operand.
   * @return A new instance of {@code BuiltInCall} representing the binary operation.
   */
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

  /**
   * Creates a binary operation built-in call with the specified operation and constant values.
   *
   * @param op The built-in operation to be performed.
   * @param a  The first constant value operand.
   * @param b  The second constant value operand.
   * @return A new instance of {@code BuiltInCall} representing the binary operation.
   */
  public static BuiltInCall binaryOp(BuiltInTable.BuiltIn op, ExpressionNode a, ExpressionNode b) {
    return BuiltInCall.of(op, a, b);
  }


  /**
   * Creates a binary operation built-in call with the specified operation, operand nodes,
   * and result type.
   *
   * @param op         The built-in operation to be performed.
   * @param resultType The type of the result produced by the operation.
   * @param a          The first operand expression node.
   * @param b          The second operand expression node.
   * @return A new instance of {@code BuiltInCall} representing the binary operation.
   */
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

  public static ConstantNode intSNode(long val, int width) {
    return new ConstantNode(intS(val, width));
  }

  public static ConstantNode intUNode(long val, int width) {
    return new ConstantNode(intU(val, width));
  }

  public static ConstantNode bitsNode(long val, int width) {
    return new ConstantNode(bits(val, width));
  }

  /**
   * Creates an expression tree that results in {@code true} if the value's sign is {@code 1}
   * (negative), and otherwise false.
   */
  public static ExpressionNode testSignBit(ExpressionNode value) {
    // 1001 -> msb = 3
    var msb = value.type().asDataType().bitWidth() - 1;
    // shift msb right
    var shift = BuiltInCall.of(
        BuiltInTable.LSR,
        value,
        Constant.Value.of(msb, UIntType.minimalTypeFor(msb))
            .toNode()
    );
    return new TruncateNode(shift, Type.bool());
  }

  /**
   * Creates a built-in call for {@link BuiltInTable#NOT}.
   */
  public static ExpressionNode not(ExpressionNode expr) {
    return BuiltInCall.of(
        BuiltInTable.NOT,
        expr
    );
  }

  public static ExpressionNode and(ExpressionNode... exprs) {
    return combineExpressions(BuiltInTable.AND, exprs);
  }

  public static ExpressionNode or(ExpressionNode... exprs) {
    return combineExpressions(BuiltInTable.OR, exprs);
  }

  public static ExpressionNode equ(ExpressionNode a, ExpressionNode b) {
    return combineExpressions(BuiltInTable.EQU, a, b);
  }

  public static ExpressionNode neq(ExpressionNode a, ExpressionNode b) {
    return combineExpressions(BuiltInTable.NEQ, a, b);
  }

  public static ExpressionNode sub(ExpressionNode... exprs) {
    return combineExpressions(BuiltInTable.SUB, exprs);
  }

  public static ExpressionNode add(ExpressionNode... exprs) {
    return combineExpressions(BuiltInTable.ADD, exprs);
  }

  public static SelectNode select(ExpressionNode cond, ExpressionNode trueCase,
                                  ExpressionNode falseCase) {
    return new SelectNode(cond, trueCase, falseCase);
  }

  private static ExpressionNode combineExpressions(BuiltInTable.BuiltIn operation,
                                                   ExpressionNode... exprs) {
    assert exprs.length >= 2;
    var expr = BuiltInCall.of(operation, exprs[0], exprs[1]);
    for (var i = 2; i < exprs.length; i++) {
      expr = BuiltInCall.of(operation, expr, exprs[i]);
    }
    return expr;
  }


  public static ExpressionNode castBool(ExpressionNode value) {
    return binaryOp(BuiltInTable.EQU, Type.bool(), value,
        Constant.Value.of(0, value.type().asDataType()).toNode());
  }

  public static ExpressionNode signExtend(ExpressionNode value, DataType dataType) {
    return new SignExtendNode(value, dataType);
  }

  public static ExpressionNode zeroExtend(ExpressionNode value, DataType dataType) {
    return new ZeroExtendNode(value, dataType);
  }

  public static ExpressionNode truncate(ExpressionNode value, DataType dataType) {
    return new TruncateNode(value, dataType);
  }

  /**
   * Builts an if-else control flow in the given graph.
   * The {@link MergeNode} is linked to the provided next node.
   *
   * @param graph            in which it should be created
   * @param condition        of the if node
   * @param trueCaseEffects  side effects applied in the true branch
   * @param falseCaseEffects side effects applied in the false branch
   * @param next             the next control node the merge node is linked to
   * @return the created if node
   */
  public static IfNode ifElseSideEffect(Graph graph, ExpressionNode condition,
                                        List<SideEffectNode> trueCaseEffects,
                                        List<SideEffectNode> falseCaseEffects,
                                        ControlNode next) {
    var trueEnd = graph.addWithInputs(new BranchEndNode(new NodeList<>(trueCaseEffects)));
    var trueBegin = graph.addWithInputs(new BeginNode(trueEnd));
    var falseEnd = graph.addWithInputs(new BranchEndNode(new NodeList<>(falseCaseEffects)));
    var falseBegin = graph.addWithInputs(new BeginNode(falseEnd));
    var ifNode = graph.addWithInputs(new IfNode(condition, trueBegin, falseBegin));
    graph.addWithInputs(new MergeNode(new NodeList<>(trueEnd, falseEnd), next));
    return ifNode;
  }


  /// CONSTANT VALUE CONSTRUCTION ///

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
