// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.ast;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("MissingJavadocType")
public class RecordType implements SyntaxType {

  String name;
  List<Entry> entries;

  RecordType(String name, List<Entry> entries) {
    this.name = name;
    this.entries = entries;
  }

  @Override
  public String toString() {
    return name + " " + entries.stream().map(entry -> entry.type.toString())
        .collect(Collectors.joining(",", "(", ")"));
  }

  @Override
  public boolean isSubTypeOf(SyntaxType other) {
    if (!(other instanceof RecordType otherRecord)) {
      return false;
    }
    if (otherRecord.entries.size() != entries.size()) {
      return false;
    }
    for (int i = 0; i < entries.size(); i++) {
      if (!entries.get(i).type().isSubTypeOf(otherRecord.entries.get(i).type())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String print() {
    return name;
  }

  SyntaxType findEntry(String name) {
    for (Entry entry : entries) {
      if (entry.name().equals(name)) {
        return entry.type();
      }
    }
    return BasicSyntaxType.INVALID;
  }

  record Entry(String name, SyntaxType type) {
  }
}
