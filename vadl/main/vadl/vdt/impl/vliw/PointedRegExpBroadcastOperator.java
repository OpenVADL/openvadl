// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.vdt.impl.vliw;

import static vadl.vdt.impl.vliw.PointedRegExpBroadcastOperatorDispatcher.dispatch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.vliw.model.PointedExpression;
import vadl.vdt.impl.vliw.model.PointedItem;
import vadl.vdt.impl.vliw.model.PointedItem.Alternation;

/**
 * Broadcasts a point over an extended pointed regular expression (PRE).
 */
@DispatchFor(value = PointedItem.class, returnType = PointedExpression.class)
public class PointedRegExpBroadcastOperator {

  /**
   * Broadcast a point over the extended pointed item.
   *
   * @param item The pointed item.
   * @return The extended pointed regular expression.
   */
  public static PointedExpression broadcast(PointedItem item) {
    return dispatch(new PointedRegExpBroadcastOperator(), item);
  }

  /**
   * Broadcast a point over an extended PRE.
   *
   * @param expression the existing expression.
   * @return The new extended PRE.
   */
  public static PointedExpression broadcast(PointedExpression expression) {
    final var nExpr = dispatch(new PointedRegExpBroadcastOperator(), expression.item());
    return new PointedExpression(nExpr.item(), expression.trailing() || nExpr.trailing());
  }

  @Handler
  public PointedExpression handle(PointedItem.Literal literal) {
    final var item = new PointedItem.Literal(literal.lit(), true);
    return new PointedExpression(item, false);
  }

  @Handler
  public PointedExpression handle(PointedItem.Alternation alternation) {

    final List<PointedExpression> subEx = alternation.elements()
        .stream()
        .map(se -> dispatch(this, se))
        .toList();

    final List<PointedItem> items = subEx.stream()
        .map(PointedExpression::item)
        .toList();
    final boolean withTrailing = subEx.stream()
        .anyMatch(PointedExpression::trailing);

    return new PointedExpression(
        new Alternation(items), withTrailing
    );
  }

  @Handler
  public PointedExpression handle(PointedItem.Sequence sequence) {

    final List<PointedItem> items = new ArrayList<>();

    boolean trailing = true;
    for (var elem : sequence.elements()) {

      if (!trailing) {
        // Don't broadcast further
        items.add(elem);
        continue;
      }

      // Broadcast over the subexpression
      final var expr = dispatch(this, elem);
      items.add(expr.item());
      trailing = expr.trailing();
    }

    return new PointedExpression(new PointedItem.Sequence(items), trailing);
  }

  @Handler
  public PointedExpression handle(PointedItem.Permutation permutation) {

    final boolean allTrailing = permutation.elements()
        .stream()
        .map(se -> dispatch(this, se))
        .allMatch(PointedExpression::trailing);

    return new PointedExpression(
        new PointedItem.Permutation(permutation.elements(), permutation.counter(), true),
        allTrailing
    );
  }

  @Handler
  public PointedExpression handle(PointedItem.Repetition repetition) {
    return new PointedExpression(
        new PointedItem.Repetition(repetition.expr(), repetition.from(), repetition.to(),
            repetition.counter(), true),
        repetition.from().compareTo(BigInteger.ZERO) <= 0);
  }
}
