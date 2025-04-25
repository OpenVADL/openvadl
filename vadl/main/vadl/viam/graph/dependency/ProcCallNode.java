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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.ExceptionDef;
import vadl.viam.Procedure;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A procedure call calls a {@link Procedure}.
 * It is a {@link SideEffectNode}, as it may contain resource writes, but has no return value.
 */
public class ProcCallNode extends SideEffectNode {

  @DataValue
  Procedure procedure;

  @Input
  NodeList<ExpressionNode> arguments;

  /**
   * Construct the procedure call node.
   */
  public ProcCallNode(Procedure procedure, NodeList<ExpressionNode> arguments,
                      @Nullable ExpressionNode condition) {
    super(condition);
    this.procedure = procedure;
    this.arguments = arguments;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(arguments.size() == procedure.parameters().length, "Wrong number of arguments");
    var params = procedure.parameters();
    ensure(
        IntStream.range(0, arguments.size() - 1)
            .allMatch(
                i -> arguments.get(i).type().isTrivialCastTo(params[i].type())),
        "Parameter fields do not match concrete argument fields"
    );
  }

  public Procedure procedure() {
    return procedure;
  }

  public NodeList<ExpressionNode> arguments() {
    return arguments;
  }

  /**
   * Indicates whether this call represents an exception raise.
   */
  public boolean exceptionRaise() {
    return procedure instanceof ExceptionDef;
  }

  @Override
  public Node copy() {
    return new ProcCallNode(procedure, arguments.copy(),
        condition != null ? condition.copy() : null);
  }

  @Override
  public Node shallowCopy() {
    return new ProcCallNode(procedure, arguments, condition);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // do nothing
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(procedure);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(arguments);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arguments = arguments.stream().map(e ->
            visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}
