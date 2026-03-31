// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.types;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nonnull;
import vadl.viam.ViamError;

/**
 * TypeList is a specialized ArrayList that only allows elements of a specific type.
 * It provides additional methods to ensure the type and length constraints of the list.
 *
 * @param <T> the type of elements in the list, must extend Type
 */
public class TypeList<T extends Type> extends ArrayList<T> {

  public TypeList(int initialCapacity) {
    super(initialCapacity);
  }

  public TypeList() {
  }

  public TypeList(@Nonnull Collection<? extends T> c) {
    super(c);
  }

  public T first() {
    return get(0);
  }

  public T second() {
    return get(1);
  }

  /**
   * Ensures that all elements in the TypeList are of the specified types.
   *
   * @param types  the array of types that all elements should be of
   * @param format the format string to be used when creating the error message
   * @param args   the arguments to be used when formatting the error message
   * @throws ViamError if any element in the TypeList does not match any of the specified types
   */
  @FormatMethod
  public void ensureAllOfType(Type[] types, String format, Object... args) {
    for (var t : this) {
      boolean found = false;
      for (var type : types) {
        if (t.equals(type)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new ViamError("invalid type in list: " + format.formatted(args))
            .addContext("type", t.toString())
            .addContext("list", this.toString())
            .shrinkStacktrace(1);
      }
    }
  }

  /**
   * Ensures that the length of the TypeList is equal to the specified length.
   *
   * @param len    the expected length of the list
   * @param format the format string to be used when creating the error message
   * @param args   the arguments to be used when formatting the error message
   * @throws ViamError if the length of the TypeList is not equal to the expected length
   */
  @FormatMethod
  public void ensureLength(int len, String format, Object... args) {
    if (len != this.size()) {
      throw new ViamError(
          format.formatted(args) + ": invalid length of list. Expected " + len + ", got "
              + this.size())
          .addContext("expectedLength", len)
          .addContext("list", this.toString())
          .shrinkStacktrace(1);

    }
  }

  @FormatMethod
  public void ensureAllOfLength(int len, String format, Object... args) {

  }


}
