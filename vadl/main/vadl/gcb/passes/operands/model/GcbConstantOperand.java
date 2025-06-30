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

import static vadl.viam.ViamError.ensure;

import vadl.viam.Constant;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Indicates that the operand is a {@link Constant} index of a register file. It
 * can be only lowered when the register file at that constant is also a constant.
 */
public final class GcbConstantOperand extends GcbInstructionOperand {
  private final Constant constant;

  /**
   * Constructor.
   */
  public GcbConstantOperand(ConstantNode constantNode, Constant value) {
    super(constantNode);
    ensure(constantNode.constant().equals(value),
        "This is definitely wrong because index and constraint value are mismatched");
    this.constant = value;
  }

  public Constant constant() {
    return constant;
  }
}
