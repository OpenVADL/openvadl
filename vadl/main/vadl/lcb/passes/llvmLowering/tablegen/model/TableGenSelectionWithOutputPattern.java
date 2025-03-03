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

package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.graph.Graph;

/**
 * This is tablegen pattern which has a selector and emits a machine instruction or pseudo
 * instruction.
 */
public class TableGenSelectionWithOutputPattern extends TableGenPattern {

  private final Graph machine;

  public TableGenSelectionWithOutputPattern(Graph selector, Graph machine) {
    super(selector);
    this.machine = machine;
  }

  /**
   * Copy the {@code selector} and {@link #machine} and create new object.
   */
  @Override
  public TableGenPattern copy() {
    return new TableGenSelectionWithOutputPattern(selector.copy(), machine.copy());
  }

  public Graph machine() {
    return machine;
  }
}
