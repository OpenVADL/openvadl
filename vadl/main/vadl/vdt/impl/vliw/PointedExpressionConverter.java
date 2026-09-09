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

import static vadl.error.Diagnostic.error;
import static vadl.vdt.impl.vliw.PointedExpressionConverterDispatcher.dispatch;

import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.vliw.model.PointedExpression;
import vadl.vdt.impl.vliw.model.PointedItem;
import vadl.vdt.impl.vliw.model.PointedItem.Alternation;
import vadl.vdt.impl.vliw.model.PointedItem.Sequence;
import vadl.viam.Group;
import vadl.viam.Group.Expression;

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

    return alternation.elements().stream()
        .map(se -> dispatch(this, ctx, se))
        .reduce(new PointedExpression(new Alternation(new ArrayList<>()), false),
            (a, b) -> {

              if (!(a.item() instanceof Alternation(List<PointedItem> aElems))) {
                throw error("Failed to construct group expression", alternation.location())
                    .build();
              }

              final var combinedElems = new ArrayList<>(aElems);
              combinedElems.add(b.item());
              final var combinedItem = new Alternation(combinedElems);
              return new PointedExpression(combinedItem, a.trailing() || b.trailing());
            });
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Sequence sequence) {

    return sequence.elements().stream()
        .map(se -> dispatch(this, ctx, se))
        .reduce(new PointedExpression(new Sequence(new ArrayList<>()), false),
            (a, b) -> {

              if (!(a.item() instanceof Sequence(List<PointedItem> aElems))) {
                throw error("Failed to construct group expression", sequence.location())
                    .build();
              }

              final var combinedElems = new ArrayList<>(aElems);
              combinedElems.add(b.item());

              final var combinedItem = new Alternation(combinedElems);
              return new PointedExpression(combinedItem, a.trailing() || b.trailing());
            });
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Permutation permutation) {
    return null;
  }

  @Handler
  public PointedExpression handle(Context ctx, Group.Repetition repetition) {
    return null;
  }
}
