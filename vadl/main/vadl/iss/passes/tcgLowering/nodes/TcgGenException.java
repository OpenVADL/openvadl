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

package vadl.iss.passes.tcgLowering.nodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.ExceptionDef;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A TCG node that represents the {@code generate_exception} function to raise an exception
 * in TCG.
 * This substitutes the {@code raise} statement in VADL.
 */
public class TcgGenException extends TcgNode {

  @DataValue
  ExceptionDef exception;

  @Input
  NodeList<TcgVRefNode> args;

  public TcgGenException(ExceptionDef exception, NodeList<TcgVRefNode> args) {
    this.exception = exception;
    this.args = args;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    // update PC before leaving TB and generate
    return "gen_update_pc_diff(ctx, 0);\n\t"
        + "gen_helper_raise_" + exception.simpleName().toLowerCase() + "(tcg_env, "
        + args.stream().map(nodeToCCode).collect(Collectors.joining(", "))
        + ")";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return new HashSet<>(args);
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }

  @Override
  public Node copy() {
    return new TcgGenException(exception, args.copy());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(exception);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(args);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    args = args.stream().map(e ->
            visitor.apply(this, e, TcgVRefNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}
