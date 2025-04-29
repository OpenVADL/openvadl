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

import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Node;

/**
 * A user can specify a concrete register in a {@link PseudoInstruction}.
 */
public class TableGenInstructionConcreteRegisterOperand extends TableGenInstructionOperand {
  private final RegisterTensor registerTensor;
  private final int address;

  /**
   * Constructor.
   */
  public TableGenInstructionConcreteRegisterOperand(RegisterTensor registerTensor,
                                                    int address,
                                                    Node origin) {
    super(origin);
    this.registerTensor = registerTensor;
    this.address = address;
  }

  @Override
  public String render() {
    return registerTensor.simpleName() + address;
  }
}
