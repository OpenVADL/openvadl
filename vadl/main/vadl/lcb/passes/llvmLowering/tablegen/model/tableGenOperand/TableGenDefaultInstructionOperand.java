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

package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import java.util.Objects;
import vadl.viam.graph.Node;

/**
 * The default tablegen instruction operand has always a type and a name.
 */
public class TableGenDefaultInstructionOperand extends TableGenInstructionOperand {
  private final String type;
  private final String name;

  /**
   * Constructor.
   */
  public TableGenDefaultInstructionOperand(Node origin, String type, String name) {
    super(origin);
    this.type = type;
    this.name = name;
  }

  @Override
  public String render() {
    return String.format("%s:$%s", type, name);
  }

  public String type() {
    return type;
  }

  public String name() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(origin, name());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableGenDefaultInstructionOperand that = (TableGenDefaultInstructionOperand) o;
    return Objects.equals(type, that.type) && Objects.equals(name, that.name);
  }
}
