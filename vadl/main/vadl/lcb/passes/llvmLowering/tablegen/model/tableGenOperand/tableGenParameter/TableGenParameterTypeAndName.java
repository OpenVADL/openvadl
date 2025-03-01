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

package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter;

import java.util.Objects;

/**
 * Parameter with a type and a name of a {@link TableGenParameter}.
 */
public class TableGenParameterTypeAndName extends TableGenParameter {
  private final String type;
  private final String name;

  public TableGenParameterTypeAndName(String type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public String render() {
    return String.format("%s:$%s", type, name);
  }

  public TableGenParameterTypeAndName withType(String type) {
    return new TableGenParameterTypeAndName(type, name);
  }

  public String name() {
    return name;
  }

  public String type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o instanceof TableGenParameterTypeAndName casted) {
      return type.equals(casted.type) && name.equals(casted.name);
    }

    return false;
  }


  @Override
  public int hashCode() {
    int result = Objects.hashCode(type);
    result = 31 * result + Objects.hashCode(name);
    return result;
  }
}
