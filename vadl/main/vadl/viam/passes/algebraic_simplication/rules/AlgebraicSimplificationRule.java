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

package vadl.viam.passes.algebraic_simplication.rules;

import java.util.Optional;
import vadl.types.DataType;
import vadl.types.TupleType;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents that a class implements an algebraic simplification.
 */
public interface AlgebraicSimplificationRule {
  /**
   * Check and apply an algebraic simplification.
   *
   * @param node is {@link Node} where the check is applied on.
   * @return {@link Optional} when it can be replaced and {@code empty} when not.
   */
  Optional<Node> simplify(Node node);

  /**
   * Get the type of the given {@code node} when it has a {@link DataType}.
   * When the {@code node} has a {@link TupleType} then return the type of the first
   * child.
   */
  default DataType getType(ExpressionNode node) {
    if (node.type() instanceof TupleType tupleType) {
      return tupleType.first().asDataType();
    }

    return node.type().asDataType();
  }
}
