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

import java.util.List;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.graph.Graph;

/**
 * This is tablegen pattern defines a pseudo instruction expansion.
 */
public class TableGenPseudoInstExpansionPattern extends TableGenPattern {

  private final String name;
  private final boolean isCall;
  private final boolean isBranch;
  private final boolean isIndirectBranch;
  private final boolean isTerminator;
  private final boolean isBarrier;
  private final List<TableGenInstructionOperand> outputs;
  private final List<TableGenInstructionOperand> inputs;
  private final List<RegisterRef> defs;
  private final Graph machine;

  /**
   * Constructor.
   */
  public TableGenPseudoInstExpansionPattern(String name,
                                            Graph selector,
                                            Graph machine,
                                            boolean isCall,
                                            boolean isBranch,
                                            boolean isIndirectBranch,
                                            boolean isTerminator,
                                            boolean isBarrier,
                                            List<TableGenInstructionOperand> inputs,
                                            List<TableGenInstructionOperand> outputs,
                                            List<RegisterRef> defs) {
    super(selector);
    this.name = name;
    this.isCall = isCall;
    this.isBranch = isBranch;
    this.isIndirectBranch = isIndirectBranch;
    this.isTerminator = isTerminator;
    this.isBarrier = isBarrier;
    this.machine = machine;
    this.inputs = inputs;
    this.outputs = outputs;
    this.defs = defs;
  }

  /**
   * Copy the {@code selector} and {@link #machine} and create new object.
   */
  @Override
  public TableGenPattern copy() {
    return new TableGenPseudoInstExpansionPattern(name, selector.copy(), machine.copy(), isCall,
        isBranch,
        isIndirectBranch,
        isTerminator,
        isBarrier,
        inputs,
        outputs,
        defs);
  }

  public Graph machine() {
    return machine;
  }

  public String name() {
    return name;
  }

  public boolean isCall() {
    return isCall;
  }

  public boolean isBranch() {
    return isBranch;
  }

  public boolean isIndirectBranch() {
    return isIndirectBranch;
  }

  public boolean isTerminator() {
    return isTerminator;
  }

  public boolean isBarrier() {
    return isBarrier;
  }

  public List<TableGenInstructionOperand> outputs() {
    return outputs;
  }

  public List<TableGenInstructionOperand> inputs() {
    return inputs;
  }

  public List<RegisterRef> defs() {
    return defs;
  }
}
