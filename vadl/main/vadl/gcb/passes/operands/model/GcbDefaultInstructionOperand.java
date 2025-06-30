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

package vadl.gcb.passes.operands.model;

import vadl.viam.graph.Node;

/**
 * The default instruction operand has always a type and a name.
 */
public sealed class GcbDefaultInstructionOperand extends GcbInstructionOperand
    permits GcbFieldAccessOperand, GcbInstructionBareSymbolOperand,
    GcbInstructionIndexRegisterFileOperand, GcbRegisterFileOperand {
  private final String type;
  private final String name;

  protected GcbDefaultInstructionOperand(Node origin, String type, String name) {
    super(origin);
    this.type = type;
    this.name = name;
  }

  public String type() {
    return type;
  }

  public String name() {
    return name;
  }
}
