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

package vadl.viam.passes.algebraic_simplication.rules.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

/**
 * Simplification rule when division with one then return the first operand of
 * the multiplication.
 */
public class DivisionWithOneSimplificationRule implements AlgebraicSimplificationRule {
  @Override
  public Optional<Node> simplify(Node node) {
    if (node instanceof ExpressionNode n) {
      var matcher =
          new BuiltInMatcher(
              List.of(BuiltInTable.SDIV, BuiltInTable.UDIV),
              List.of(new AnyNodeMatcher(), new ConstantValueMatcher(
                  Constant.Value.of(1, getType(n)))));

      var matchings = TreeMatcher.matches(Stream.of(node), matcher);
      if (!matchings.isEmpty()) {
        return Optional.ofNullable(n.inputs().toList().get(0));
      }
    }
    return Optional.empty();
  }
}
