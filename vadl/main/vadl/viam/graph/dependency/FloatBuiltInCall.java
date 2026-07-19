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

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.FloatType;
import vadl.types.Type;
import vadl.viam.FloatFormat;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a function call to a VADL float built-in.
 * It holds a {@link BuiltIn} function from the {@link BuiltInTable} and
 * extends {@link BuiltInCall}.
 *
 * @see BuiltInCall
 * @see BuiltInTable
 * @see AbstractFunctionCallNode
 */
public class FloatBuiltInCall extends BuiltInCall {

  @DataValue
  protected List<FloatFormat> formats;

  public FloatBuiltInCall(BuiltIn builtIn, NodeList<ExpressionNode> args,
                          List<FloatFormat> formats, Type type) {
    super(builtIn, args, type);
    this.formats = formats;
  }

  public List<FloatFormat> formats() {
    return formats;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(builtIn.signature().floatTypeArgCount() == formats().size(),
        "Number of float types must match, %s vs %s",
        builtIn.signature().floatTypeArgCount(), formats().size());
  }

  @Override
  public ExpressionNode copy() {
    return new FloatBuiltInCall(builtIn,
        new NodeList<>(arguments().stream().map(ExpressionNode::copy).toList()),
        formats(),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new FloatBuiltInCall(builtIn, args, formats(), type());
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formats);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(builtIn.name());
    sb.append("(");

    for (int i = 0; i < args.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      args.get(i).prettyPrint(sb);
    }

    for (int i = 0; i < formats.size(); i++) {
      if (i > 0 || !args.isEmpty()) {
        sb.append(", ");
      }
      formats.get(i).simpleName();
    }

    sb.append(")");
  }
}
