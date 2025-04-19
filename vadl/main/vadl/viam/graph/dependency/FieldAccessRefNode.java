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
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a node that holds a reference to a format field access.
 */
public class FieldAccessRefNode extends ParamNode {

  @DataValue
  protected Format.FieldAccess fieldAccess;

  /**
   * Creates an FieldAccessRefNode object that holds a reference to a format field access.
   *
   * @param fieldAccess the format immediate to be referenced
   */
  public FieldAccessRefNode(Format.FieldAccess fieldAccess, Type type) {
    super(type);

    this.fieldAccess = fieldAccess;
  }

  public Format.FieldAccess fieldAccess() {
    return fieldAccess;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fieldAccess);
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(fieldAccess.type().isTrivialCastTo(type()),
        "Type of fieldAccess can't be trivially cast to node's type. %s vs %s", fieldAccess.type(),
        type());

  }

  @Override
  public ExpressionNode copy() {
    return new FieldAccessRefNode(fieldAccess, type());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Definition definition() {
    return fieldAccess;
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(fieldAccess.simpleName());
  }
}
