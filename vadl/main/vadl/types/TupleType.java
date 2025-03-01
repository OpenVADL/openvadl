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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that represents a tuple type in VADL containing a list of subtypes.
 */
public class TupleType extends Type {

  private final List<Type> types;

  protected TupleType(Type... types) {
    this.types = Arrays.asList(types);
  }

  public Type first() {
    // TODO: Ensure
    return types.get(0);
  }

  public Type last() {
    // TODO: Ensure
    return types.get(types.size() - 1);
  }

  public Type get(int i) {
    return types.get(i);
  }

  public Stream<Type> types() {
    return types.stream();
  }

  public int size() {
    return types.size();
  }

  @Override
  public String name() {
    return "(%s)".formatted(types.stream()
        .map(Type::name)
        .collect(Collectors.joining(", ")));
  }

}
