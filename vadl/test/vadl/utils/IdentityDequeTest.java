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

package vadl.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IdentityDequeTest {

  private record Key(int value) {
  }

  @Test
  public void containsUsesIdentity() {
    var value = new Key(1);
    var equalValue = new Key(1);
    var deque = new IdentityDeque<Key>();

    deque.add(value);

    assertTrue(deque.contains(value));
    assertFalse(deque.contains(equalValue));
  }

  @Test
  public void removeUsesIdentity() {
    var value = new Key(1);
    var equalValue = new Key(1);
    var deque = new IdentityDeque<Key>();

    deque.add(value);

    assertFalse(deque.remove(equalValue));
    assertTrue(deque.remove(value));
    assertTrue(deque.isEmpty());
  }

  @Test
  public void removeOccurrenceMethodsUseIdentity() {
    var value = new Key(1);
    var equalValue = new Key(1);
    var deque = new IdentityDeque<Key>();

    deque.add(value);
    deque.add(equalValue);
    deque.add(value);

    assertTrue(deque.removeLastOccurrence(value));
    assertSame(value, deque.getFirst());
    assertSame(equalValue, deque.getLast());

    assertTrue(deque.removeFirstOccurrence(value));
    assertSame(equalValue, deque.getFirst());
  }

}
