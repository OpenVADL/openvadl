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

import java.util.ArrayList;
import java.util.List;

/**
 * A immutable rope list implementation.
 * The container enables especially fast O(1) concatenations and O(n) lookups.
 * Since the list is immutable, there is no way to add nor delete items but sublists can be shared,
 * across many instances.
 *
 * @param <T> of the items it holds.
 */
public sealed interface RopeList<T> permits RopeList.ItemLeaf, RopeList.ListLeaf, RopeList.Concat {

  /**
   * A single element list.
   *
   * @param element it holds.
   */
  record ItemLeaf<T>(T element) implements RopeList<T> {
  }

  /**
   * A list of elements.
   *
   * @param elements it holds.
   */
  record ListLeaf<T>(List<T> elements) implements RopeList<T> {
  }

  /**
   * Concatenates two lists.
   *
   * @param left  first list it holds.
   * @param right second list it holds
   */
  record Concat<T>(RopeList<T> left, RopeList<T> right) implements RopeList<T> {
  }

  /**
   * Create a new rope list with a single element.
   *
   * @param element to include in the list.
   * @return a new rope list containing the element.
   */
  static <T> RopeList<T> of(T element) {
    return new ItemLeaf<>(element);
  }

  /**
   * Create a new rope list from a variable number of elements.
   *
   * @param elements to include in the list.
   * @return a new rope list containing the elements.
   */
  static <T> RopeList<T> of(T... elements) {
    if (elements.length == 1) {
      return new ItemLeaf<>(elements[0]);
    }
    return new ListLeaf<>(List.of(elements));
  }


  /**
   * Create a new rope list from a list of elements.
   *
   * @param elements to include in the list.
   * @return a new rope list containing the elements.
   */
  static <T> RopeList<T> of(List<T> elements) {
    if (elements.size() == 1) {
      return new ItemLeaf<>(elements.get(0));
    }
    return new ListLeaf<>(elements);
  }

  /**
   * Get the first element stored in the list.
   *
   * @return the element.
   */
  default T getFirst() {
    return switch (this) {
      case ItemLeaf<T> node -> node.element;
      case Concat<T> node -> node.left.getFirst();
      case ListLeaf<T> node -> node.elements.getFirst();
    };
  }

  /**
   * Get the last element stored in the list.
   *
   * @return the element.
   */
  default T getLast() {
    return switch (this) {
      case ItemLeaf<T> node -> node.element;
      case Concat<T> node -> node.left.getLast();
      case ListLeaf<T> node -> node.elements.getLast();
    };
  }

  /**
   * Concatenates two lists in constant time and space.
   * Does not modify the original lists.
   *
   * @param other list ot concat.
   * @return a new list that contains both.
   */
  default RopeList<T> concat(RopeList<T> other) {
    return new Concat<>(this, other);
  }

  /**
   * Converts the rope to a list.
   *
   * @return a list containing all elements of the rope.
   */
  default List<T> toList() {
    var result = new ArrayList<T>();
    flatten(result);
    return result;
  }

  /**
   * Checks whether the list contains a given element.
   *
   * @param needle to search in the rope.
   * @return true if the list contains the needle, false otherwise.
   */
  default boolean contains(T needle) {
    return switch (this) {
      case ItemLeaf<T>(var elem) -> needle.equals(elem);
      case ListLeaf<T>(var elems) -> elems.contains(needle);
      case Concat<T>(var l, var r) -> l.contains(needle) || r.contains(needle);
    };
  }

  private void flatten(List<T> acc) {
    switch (this) {
      case ItemLeaf<T>(var elem) -> acc.add(elem);
      case ListLeaf<T>(var elems) -> acc.addAll(elems);
      case Concat<T>(var l, var r) -> {
        l.flatten(acc);
        r.flatten(acc);
      }
    }
  }

}