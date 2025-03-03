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

package vadl.viam.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.types.TypeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A list of nodes in the VIAM graph.
 */
public class NodeList<T extends Node> extends ArrayList<T> {

  public NodeList() {
  }

  public NodeList(@Nonnull Collection<? extends T> c) {
    super(c);
  }

  @SafeVarargs
  public NodeList(@Nonnull T... c) {
    this(Arrays.stream(c).toList());
  }

  /**
   * Converts the NodeList to a TypeList containing the types of the ExpressionNodes.
   *
   * @return a TypeList containing the types, or null if any of the nodes is not an ExpressionNode
   */
  public @Nullable TypeList<Type> toTypeList() {
    var result = new TypeList<Type>(this.size());
    for (var node : this) {
      if (!(node instanceof ExpressionNode)) {
        return null;
      }
      result.add(((ExpressionNode) node).type());
    }
    return result;
  }

  public NodeList<T> copy() {
    return new NodeList<>(this.stream().map(x -> (T) x.copy()).toList());
  }
}
