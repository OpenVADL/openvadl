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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * An {@link ArrayDeque} whose object-based lookup and removal methods compare by identity.
 *
 * @param <E> the type of elements held in this deque
 */
public class IdentityDeque<E> extends ArrayDeque<E> {

  @Override
  public boolean contains(Object o) {
    for (E element : this) {
      if (element == o) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean remove(Object o) {
    return removeFirstOccurrence(o);
  }

  @Override
  public boolean removeFirstOccurrence(Object o) {
    for (Iterator<E> iterator = iterator(); iterator.hasNext(); ) {
      if (iterator.next() == o) {
        iterator.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    for (Iterator<E> iterator = descendingIterator(); iterator.hasNext(); ) {
      if (iterator.next() == o) {
        iterator.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object element : c) {
      if (!contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    var identities = identityLookup(c);
    var modified = false;
    for (Iterator<E> iterator = iterator(); iterator.hasNext(); ) {
      if (identities.containsKey(iterator.next())) {
        iterator.remove();
        modified = true;
      }
    }
    return modified;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    var identities = identityLookup(c);
    var modified = false;
    for (Iterator<E> iterator = iterator(); iterator.hasNext(); ) {
      if (!identities.containsKey(iterator.next())) {
        iterator.remove();
        modified = true;
      }
    }
    return modified;
  }

  private static IdentityHashMap<Object, Void> identityLookup(Collection<?> collection) {
    var identities = new IdentityHashMap<Object, Void>(collection.size());
    for (Object element : collection) {
      identities.put(element, null);
    }
    return identities;
  }
}
