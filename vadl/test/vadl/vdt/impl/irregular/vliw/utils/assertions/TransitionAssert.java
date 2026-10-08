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

package vadl.vdt.impl.irregular.vliw.utils.assertions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import vadl.vdt.impl.vliw.model.Transition;

public class TransitionAssert {

  private final TransitionSetAssert parent;
  private final Set<Transition> available;
  private Set<Transition> matching;

  private String expectedState;
  private Set<String> expectedActions;
  private String expectedConditions;

  TransitionAssert(TransitionSetAssert parent, Set<Transition> transition) {
    this.parent = parent;
    this.available = transition;
    this.matching = this.available;
  }

  TransitionAssert(TransitionSetAssert parent, Transition transition) {
    this.parent = parent;
    this.available = Set.of(transition);
    this.matching = this.available;
  }

  public static TransitionAssert assertThat(Set<Transition> transitions) {
    return new TransitionAssert(null, transitions);
  }

  public static TransitionAssert assertThat(Transition transition) {
    return new TransitionAssert(null, transition);
  }

  public TransitionAssert next() {
    return done().transition();
  }

  public TransitionSetAssert done() {
    Assertions.assertNotNull(parent);
    if (matching.size() != 1) {
      Assertions.fail("Multiple transitions match the expected criteria.");
    }
    parent.recordMatch(matching.stream().findFirst().get());
    return parent;
  }

  public TransitionAssert hasState(String expected) {

    if (expectedState != null) {
      Assertions.fail("Cannot assert multiple target states on a transition");
    }
    expectedState = expected;

    matching = matching.stream()
        .filter(t -> expected.equals(t.nextState().toString(true)))
        .collect(Collectors.toSet());

    if (matching.isEmpty()) {
      Assertions.fail(getErrorMessage());
    }

    return this;
  }

  public TransitionAssert hasActions(String... expected) {
    if (expectedActions != null) {
      Assertions.fail(
          "Cannot assert multiple target actions (multiple calls to #hasActions) on a transition");
    }
    expectedActions = new LinkedHashSet<>(Stream.of(expected).toList());

    matching = matching.stream()
        .filter(t -> expectedActions
            .equals(t.actions().stream()
                .map(Object::toString)
                .collect(Collectors.toSet())))
        .collect(Collectors.toSet());

    if (matching.isEmpty()) {
      Assertions.fail(getErrorMessage());
    }

    return this;
  }

  public TransitionAssert noActions() {
    if (expectedActions != null) {
      Assertions.fail(
          "Cannot assert multiple target actions (multiple calls to #hasActions) on a transition");
    }
    expectedActions = Set.of();

    matching = matching.stream()
        .filter(t -> t.actions().isEmpty())
        .collect(Collectors.toSet());

    if (matching.isEmpty()) {
      Assertions.fail(getErrorMessage());
    }

    return this;
  }

  public TransitionAssert hasConditions(String... expected) {
    // TODO: Implement
    return this;
  }

  private String getErrorMessage() {
    var msg = new StringBuilder();
    msg.append("Expected a transition ");

    List<String> explanations = new ArrayList<>();
    if (expectedState != null) {
      explanations.add(
          "with state " + expectedState
      );
    }
    if (expectedActions != null) {
      explanations.add(
          "with actions " + expectedActions
      );
    }
    if (expectedConditions != null) {
      explanations.add(
          "with conditions " + expectedConditions
      );
    }
    msg.append(String.join(", ", explanations));

    msg.append(", but got ");
    msg.append(available);
    return msg.toString();
  }

}
