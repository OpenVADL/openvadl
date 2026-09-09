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

import static vadl.vdt.impl.vliw.PointedExpressionConverterDispatcher.dispatch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.vliw.model.PointedExpression;
import vadl.vdt.impl.vliw.model.PointedItem;
import vadl.vdt.impl.vliw.model.PointedItem.Alternation;
import vadl.viam.Group;
import vadl.viam.Group.Expression;

/**
 * Converts a VLIW group expression to an extended pointed regular expression. It implements
 * the initial broadcast operation.
 */
@DispatchFor(value = Expression.class, returnType = PointedExpression.class, context = PointedExpressionConverter.Context.class)
public class PointedExpressionConverter {

  public record Context(boolean withPoint) {
  }

  public static PointedExpression initialize(Expression expression) {
    return dispatch(new PointedExpressionConverter(), new Context(true), expression);
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Literal literal) {
    final var item = new PointedItem.Literal(literal, ctx.withPoint());
    return new PointedExpression(item, false);
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Alternation alternation) {

    final List<PointedExpression> subEx = alternation.elements()
        .stream()
        .map(se -> dispatch(this, ctx, se))
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
  public PointedExpression handle(Context ctx, Group.Sequence sequence) {

    final List<PointedExpression> exprs = new ArrayList<>();

    for (var elem : sequence.elements()) {
      final var expr = dispatch(this, new Context(
          exprs.isEmpty() ? ctx.withPoint() : exprs.getLast().trailing()
      ), elem);
      exprs.add(expr);
    }

    final var items = exprs.stream()
        .map(PointedExpression::item)
        .toList();
    return new PointedExpression(
        new PointedItem.Sequence(items),
        exprs.getLast().trailing());
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Permutation permutation) {

    final List<PointedItem> unpointed = permutation.elements()
        .stream()
        .map(se -> dispatch(this, new Context(false), se))
        .map(PointedExpression::item)
        .toList();

    if (!ctx.withPoint()) {
      return new PointedExpression(
          new PointedItem.Permutation(unpointed, false), false
      );
    }

    final boolean allTrailing = permutation.elements()
        .stream()
        .map(se -> dispatch(this, new Context(true), se))
        .allMatch(PointedExpression::trailing);

    return new PointedExpression(
        new PointedItem.Permutation(unpointed, true), allTrailing
    );
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Repetition repetition) {

    return new PointedExpression(
        new PointedItem.Repetition(
            dispatch(this, new Context(false), repetition.expression()).item(),
            repetition.from().integer(),
            repetition.to().integer(),
            ctx.withPoint()
        ),
        repetition.from().integer().compareTo(BigInteger.ZERO) <= 0
    );

  }
}
