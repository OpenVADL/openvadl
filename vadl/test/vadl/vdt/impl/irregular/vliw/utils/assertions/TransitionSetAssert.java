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

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import vadl.vdt.impl.vliw.model.Transition;

public class TransitionSetAssert {
  final Set<Transition> actual;
  final Set<Transition> matched = new LinkedHashSet<>();

  public TransitionSetAssert(Set<Transition> actual) {
    this.actual = new LinkedHashSet<>(actual);
  }

  public static TransitionSetAssert assertThat(Set<Transition> actual) {
    return new TransitionSetAssert(actual);
  }

  void recordMatch(Transition t) {
    if (matched.contains(t)) {
      Assertions.fail("This transition was already matched by a previous assertion");
    }
    matched.add(t);
  }

  public TransitionSetAssert size(int expected) {
    Assertions.assertEquals(expected, actual.size());
    return this;
  }

  public TransitionAssert transition() {
    return new TransitionAssert(this, actual);
  }

  public void noOthers() {
    Assertions.assertEquals(matched.size(), actual.size(),
        "Expected %d transitions but got %d".formatted(matched.size(), actual.size()));
  }
}
