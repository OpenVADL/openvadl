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

package vadl.viam.graph.dependency;

import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a function call to a VADL built-in.
 * It holds a {@link BuiltIn} function from the {@link vadl.types.BuiltInTable}.
 *
 * @see vadl.types.BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class BuiltInCall extends AbstractFunctionCallNode implements Canonicalizable {

  @DataValue
  protected BuiltIn builtIn;

  @DataValue
  protected List<Type> typeParams;

  /**
   * Stores the original types of all arguments, since the types stored in the argument
   * nodes may be altered or normalized (to BitsType). May be {@code null}, if the original
   * was not preserved.
   */
  @DataValue
  @Nullable
  protected List<Type> originalArgTypes;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public BuiltInCall(BuiltIn builtIn, List<Type> typeParams, @Nullable List<Type> originalArgTypes,
                     NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.builtIn = builtIn;
    this.typeParams = typeParams;
    this.originalArgTypes = originalArgTypes;
  }

  public BuiltInCall(BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    this(builtIn, List.of(), null, args, type);
  }

  /**
   * Utility constructor for a built-in call.
   */
  public static BuiltInCall of(BuiltIn builtIn, ExpressionNode... args) {
    return of(builtIn, List.of(args));
  }

  /**
   * Utility constructor for a built-in call.
   */
  public static BuiltInCall of(BuiltIn builtIn, List<ExpressionNode> args) {
    var argList = new NodeList<>(args);
    var type = builtIn.returns(
        args.stream()
            .map(ExpressionNode::type).toList()
    );
    return new BuiltInCall(builtIn, argList, type);
  }

  /**
   * Update the builtin by the given value.
   */
  public void setBuiltIn(BuiltIn builtIn) {
    this.builtIn = builtIn;
  }

  /**
   * Gets the {@link BuiltIn}.
   */
  public BuiltIn builtIn() {
    return this.builtIn;
  }

  /**
   * Gets the type parameters, i.e. the parameters in the pointy brackets {@code <...>}.
   */
  public List<Type> typeParams() {
    return typeParams;
  }

  /**
   * Gets the original types of the arguments.
   */
  public List<Type> originalArgTypes() {
    return requireNonNull(originalArgTypes);
  }

  public ExpressionNode arg(int index) {
    return args.get(index);
  }

  public Type typeParam(int index) {
    return typeParams.get(index);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Node canonical() {
    if (hasConstantArgs()) {
      // constant evaluation
      var args = this.arguments().stream()
          .map(x -> ((ConstantNode) x).constant())
          .toList();

      return builtIn
          .compute(typeParams, args)
          .map(e -> (Node) new ConstantNode(e))
          .orElse(this);
    }

    if (args.size() == 2) {
      // binary operation

      if (isCommutative() && args.get(0) instanceof ConstantNode) {
        // place constant node on the right side of operator
        var copy = (BuiltInCall) shallowCopy();
        // from left to right -> reverse
        reverse(copy.arguments());
        return copy;
      }
    }

    return this;
  }

  @Override
  public boolean isCommutative() {
    return BuiltInTable.COMMUTATIVE.contains(this.builtIn);
  }

  @Override
  public void verifyState() {
    super.verifyState();
    var argTypeClasses = builtIn.signature().argTypeClasses();

    ensure(argTypeClasses.size() == this.arguments().size(),
        "Number of arguments must match, %s vs %s", argTypeClasses.size(), this.arguments().size());

    var actualArgTypes = this.arguments().stream().map(ExpressionNode::type).toList();
    ensure(builtIn.takes(typeParams, actualArgTypes),
        "Arguments' types do not match with the type of the builtin. Type params: %s, Args: %s",
        typeParams, actualArgTypes);

    ensure(originalArgTypes == null || Streams.zip(
        actualArgTypes.stream(), originalArgTypes.stream(),
            (actualArgType, original) -> original.isTrivialCastTo(actualArgType)).allMatch(b -> b),
        "Arguments' types do not match with the original argument types: %s, Original: %s",
        actualArgTypes, originalArgTypes
    );

    var builtInResultType = builtIn.returns(typeParams, actualArgTypes);
    ensure(builtInResultType.isTrivialCastTo(this.type()),
        "BuiltIns' result type does not match node's type. %s vs %s", builtInResultType, this.type()
    );
  }

  @Override
  public ExpressionNode copy() {
    return new BuiltInCall(builtIn,
        new ArrayList<>(typeParams),
        originalArgTypes == null ? null : new ArrayList<>(originalArgTypes),
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        this.type());
  }

  @Override
  public Node shallowCopy() {
    return new BuiltInCall(builtIn, typeParams, originalArgTypes, args, type());
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(builtIn);
    collection.add(typeParams);
    collection.add(originalArgTypes);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    if (builtIn.operator() != null && builtIn.argTypeClasses().size() == 2) {
      sb.append("(");
      args.get(0).prettyPrint(sb);
      sb.append(" ").append(builtIn.operator()).append(" ");
      args.get(1).prettyPrint(sb);
      sb.append(")");
    } else {
      sb.append(builtIn.name());
      sb.append(typeParams.stream().map(t -> "<" + t + ">").collect(Collectors.joining()));
      sb.append("(");

      for (int i = 0; i < args.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        args.get(i).prettyPrint(sb);
      }

      sb.append(")");
    }
  }
}
