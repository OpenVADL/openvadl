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

import static vadl.vdt.impl.vliw.PointedRegExpBroadcastOperator.broadcast;
import static vadl.vdt.impl.vliw.PointedRegExpMoveOperatorDispatcher.dispatch;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.apache.commons.lang3.NotImplementedException;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.javaannotations.TypeRef;
import vadl.vdt.impl.vliw.model.Action;
import vadl.vdt.impl.vliw.model.Action.Inc;
import vadl.vdt.impl.vliw.model.Condition;
import vadl.vdt.impl.vliw.model.Condition.GreaterOrEq;
import vadl.vdt.impl.vliw.model.Condition.Less;
import vadl.vdt.impl.vliw.model.PointedExpression;
import vadl.vdt.impl.vliw.model.PointedItem;
import vadl.vdt.impl.vliw.model.PointedItem.Alternation;
import vadl.vdt.impl.vliw.model.PointedItem.Literal;
import vadl.vdt.impl.vliw.model.PointedItem.Permutation;
import vadl.vdt.impl.vliw.model.PointedItem.Repetition;
import vadl.vdt.impl.vliw.model.PointedItem.Sequence;
import vadl.vdt.impl.vliw.model.Transition;
import vadl.viam.Group;

/**
 * Consume a literal and propagate points over the extended PRE.
 */
@DispatchFor(value = PointedItem.class, returnType = PointedRegExpMoveOperator.ResultType.class)
public class PointedRegExpMoveOperator {

  /**
   * The consumed token
   */
  private final Group.Literal next;

  /**
   * The dispatcher's return type.
   */
  static class ResultType implements TypeRef<Set<Transition>> {
  }

  public PointedRegExpMoveOperator(Group.Literal next) {
    this.next = next;
  }

  public static Set<Transition> move(PointedItem item, Group.Literal atom) {
    return dispatch(new PointedRegExpMoveOperator(atom), item);
  }

  public static Set<Transition> move(PointedExpression expr, Group.Literal atom) {
    return dispatch(new PointedRegExpMoveOperator(atom), expr.item());
  }

  public Set<Transition> move(PointedItem item) {
    return dispatch(new PointedRegExpMoveOperator(next), item);
  }

  public Set<Transition> move(PointedExpression expr) {
    return dispatch(new PointedRegExpMoveOperator(next), expr.item());
  }

  @Handler
  public Set<Transition> handle(Literal literal) {

    if (!literal.pointed()) {
      final var t = new Transition(
          new PointedExpression(literal, false),
          Set.of(), Set.of()
      );
      return Set.of(t);
    }

    final var t = new Transition(
        new PointedExpression(
            // The next state is always unpointed
            new Literal(literal.lit(), false),
            // The trailing point depends on the item being the consumed token
            // TODO: Verify that this equality check works
            Objects.equals(literal.lit(), next)),
        Set.of(), Set.of()
    );

    return Set.of(t);
  }

  @Handler
  public Set<Transition> handle(Alternation alternation) {
    return combine(
        alternation.elements(),
        // Combine a sequence of two subexpression
        (a, b) ->
            new PointedExpression(
                new Alternation(
                    List.of(a.item(), b.item())
                ),
                a.trailing() || b.trailing()
            )
    );
  }

  @Handler
  public Set<Transition> handle(Sequence sequence) {
    return combine(
        sequence.elements(),
        // Combine a sequence of two subexpression
        (a, b) -> {
          if (!a.trailing()) {
            return new PointedExpression(
                new Sequence(
                    List.of(a.item(), b.item())
                ), b.trailing()
            );
          }
          final PointedExpression b2 = broadcast(b);
          return new PointedExpression(
              new Sequence(
                  List.of(a.item(), b2.item())
              ), b.trailing() || b2.trailing()
          );
        }
    );
  }

  @Handler
  public Set<Transition> handle(Permutation permutation) {
    throw new NotImplementedException("Not implemented yet");
  }

  @Handler
  public Set<Transition> handle(Repetition repetition) {
    return repetition.pointed()
        ? pointedRepetition(repetition)
        : unpointedRepetition(repetition);
  }

  private Set<Transition> unpointedRepetition(Repetition repetition) {

    final Set<Transition> moved = move(repetition.expr());
    final Set<Transition> result = new LinkedHashSet<>();

    for (Transition t : moved) {

      final var s = t.nextState();

      if (!s.trailing()) {

        // Points are still moving inside the expression
        result.add(new Transition(
            new PointedExpression(
                new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                    false),
                false
            ),
            t.actions(), t.conditions()
        ));
        continue;
      }

      // Counter is below the minimal repetitions
      final Set<Condition> c1 = new LinkedHashSet<>(t.conditions());
      c1.add(new Less(repetition.counter(), repetition.from()));

      final Set<Action> a1 = new LinkedHashSet<>(t.actions());
      a1.add(new Inc(repetition.counter()));

      result.add(new Transition(
          new PointedExpression(
              new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                  true),
              false
          ),
          a1, c1
      ));

      // Counter is above the minimal repetitions
      final Set<Condition> c2 = new LinkedHashSet<>(t.conditions());
      c2.add(new GreaterOrEq(repetition.counter(), repetition.from()));

      final Set<Action> a2 = new LinkedHashSet<>(t.actions());
      a2.add(new Inc(repetition.counter()));

      result.add(new Transition(
          new PointedExpression(
              new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                  false),
              true
          ),
          a2, c2
      ));
    }

    return result;
  }

  private Set<Transition> pointedRepetition(Repetition repetition) {

    final Set<Transition> moved = move(broadcast(repetition.expr()));

    final Set<Transition> result = new LinkedHashSet<>();
    for (Transition t : moved) {

      final var s = t.nextState();

      if (!s.trailing()) {

        // Pointer moved inside the subexpression
        result.add(new Transition(
            new PointedExpression(
                new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                    false),
                false
            ),
            // TODO: Find out what the distinction for "e' has a point" is...
            t.actions(), t.conditions()
        ));

        continue;
      }

      // Counter is below the minimal repetitions
      final Set<Condition> c1 = new LinkedHashSet<>(t.conditions());
      // TODO: check if 'from' is right, not 'from' - 1.
      c1.add(new Less(repetition.counter(), repetition.from()));

      final Set<Action> a1 = new LinkedHashSet<>(t.actions());
      a1.add(new Inc(repetition.counter()));

      result.add(new Transition(
          new PointedExpression(
              new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                  true),
              false
          ),
          a1, c1
      ));

      // Counter is above the minimal repetitions, but below the maximal reps
      final Set<Condition> c2 = new LinkedHashSet<>(t.conditions());
      c2.add(new GreaterOrEq(repetition.counter(), repetition.from()));
      c2.add(new Less(repetition.counter(), repetition.to())); // TODO: Check '-1'

      final Set<Action> a2 = new LinkedHashSet<>(t.actions());
      a1.add(new Inc(repetition.counter()));

      result.add(new Transition(
          new PointedExpression(
              new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                  true),
              true
          ),
          a2, c2
      ));

      // Counter has reached maximum reps
      final Set<Condition> c3 = new LinkedHashSet<>(t.conditions());
      c3.add(new GreaterOrEq(repetition.counter(), repetition.to()));

      final Set<Action> a3 = new LinkedHashSet<>(t.actions());
      a3.add(new Inc(repetition.counter())); // TODO: is that really necessary?

      result.add(new Transition(
          new PointedExpression(
              new Repetition(s.item(), repetition.from(), repetition.to(), repetition.counter(),
                  false),
              true
          ),
          a3, c3
      ));
    }

    return result;
  }

  private Set<Transition> combine(List<PointedItem> elements,
                                  BiFunction<PointedExpression, PointedExpression, PointedExpression> combiner) {
    Set<Transition> result = new LinkedHashSet<>();
    for (PointedItem elem : elements) {

      final Set<Transition> nextRes = move(elem);

      if (result.isEmpty()) {
        result.addAll(nextRes);
        continue;
      }

      final Set<Transition> newResult = new LinkedHashSet<>();

      for (Transition a : result) {
        for (Transition b : nextRes) {

          final PointedExpression sa = a.nextState();
          final PointedExpression sb = b.nextState();

          // Combine according to alternation or sequencing rule
          final PointedExpression combinedNextState = combiner.apply(sa, sb);

          final Set<Action> combinedActions = new LinkedHashSet<>(a.actions());
          combinedActions.addAll(b.actions());

          final Set<Condition> combinedConditions = new LinkedHashSet<>(a.conditions());
          combinedConditions.addAll(b.conditions());

          final Transition combinedTransition = new Transition(
              combinedNextState, combinedActions, combinedConditions
          );

          newResult.add(combinedTransition);
        }
      }

      result = newResult;
    }

    return result;
  }
}
