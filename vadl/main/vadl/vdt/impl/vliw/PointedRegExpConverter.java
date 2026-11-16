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

import static vadl.vdt.impl.vliw.PointedRegExpConverterDispatcher.dispatch;

import java.math.BigInteger;
import java.util.List;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.vliw.model.Counter.BitCounter;
import vadl.vdt.impl.vliw.model.Counter.IntCounter;
import vadl.vdt.impl.vliw.model.PointedExpression;
import vadl.vdt.impl.vliw.model.PointedItem;
import vadl.vdt.impl.vliw.model.PointedItem.Alternation;
import vadl.vdt.impl.vliw.model.PointedItem.Permutation;
import vadl.vdt.impl.vliw.model.PointedItem.Sequence;
import vadl.viam.Group;
import vadl.viam.Group.Expression;

/**
 * Converts a VLIW group expression to an extended pointed regular expression.
 */
@DispatchFor(value = Expression.class, returnType = PointedExpression.class)
public class PointedRegExpConverter {

  /**
   * Convert a group expression to an extended PRE.
   *
   * @param expression The group expression.
   * @return The extended PRE.
   */
  public static PointedExpression initialize(Expression expression) {
    return dispatch(new PointedRegExpConverter(), expression);
  }

  @Handler
  public PointedExpression handle(Group.Literal literal) {
    final var item = new PointedItem.Literal(literal, false);
    return new PointedExpression(item, false);
  }

  @Handler
  public PointedExpression handle(Group.Alternation alternation) {

    final List<PointedItem> subExpressions = alternation.elements()
        .stream()
        .map(se -> dispatch(this, se))
        .map(PointedExpression::item)
        .toList();
    return new PointedExpression(new Alternation(subExpressions), false);
  }

  @Handler
  public PointedExpression handle(Group.Sequence sequence) {

    final List<PointedItem> subExpressions = sequence.elements()
        .stream()
        .map(se -> dispatch(this, se))
        .map(PointedExpression::item)
        .toList();
    return new PointedExpression(new Sequence(subExpressions), false);
  }

  @Handler
  public PointedExpression handle(Group.Permutation permutation) {

    final List<PointedItem> subExpressions = permutation.elements()
        .stream()
        .map(se -> dispatch(this, se))
        .map(PointedExpression::item)
        .toList();
    return new PointedExpression(
        new Permutation(subExpressions, new BitCounter(BigInteger.valueOf(subExpressions.size())),
            false), false);
  }

  @Handler
  public PointedExpression handle(Group.Repetition repetition) {

    var subExp = dispatch(this, repetition.expression());

    return new PointedExpression(
        new PointedItem.Repetition(
            subExp.item(),
            repetition.from().integer(),
            repetition.to().integer(),
            new IntCounter(),
            false
        ),
        false
    );

  }
}
