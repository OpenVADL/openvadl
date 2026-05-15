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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;

/**
 * A class that represents a struct type in VADL containing a map of fields with their corresponding
 * type.
 */
public class StructType extends Type {

  private final SequencedMap<String, Type> types;

  protected StructType(Map<String, Type> type) {
    this.types = new LinkedHashMap<>(type);
  }

  public Type get(String field) {
    return Objects.requireNonNull(types.get(field),
        "Field '%s' does not exist in struct".formatted(field));
  }

  public boolean contains(String field) {
    return types.containsKey(field);
  }

  public SequencedMap<String, Type> fields() {
    return types;
  }

  public List<String> fieldNames() {
    return new ArrayList<>(types.keySet());
  }

  public List<Type> types() {
    return new ArrayList<>(types.values());
  }

  public int size() {
    return types.size();
  }

  @Override
  public String name() {
    final var sb = new StringBuilder("Struct<");

    boolean first = true;
    for (var entry : types.entrySet()) {
      sb
          .append(first ? "" : ", ")
          .append(entry.getKey()).append(": ")
          .append(entry.getValue().name());
      first = false;
    }

    sb.append(">");
    return sb.toString();
  }
}
