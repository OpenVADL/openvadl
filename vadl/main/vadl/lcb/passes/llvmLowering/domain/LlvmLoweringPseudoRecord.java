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

package vadl.lcb.passes.llvmLowering.domain;

import java.util.IdentityHashMap;
import java.util.List;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

/**
 * Expands {@link LlvmLoweringRecord} by the updated behaviors of the target instructions.
 */
public class LlvmLoweringPseudoRecord extends LlvmLoweringRecord {

  private final IdentityHashMap<Instruction, Graph> appliedBehaviors;

  /**
   * Constructor.
   */
  public LlvmLoweringPseudoRecord(Graph behavior,
                                  List<TableGenInstructionOperand> inputs,
                                  List<TableGenInstructionOperand> outputs,
                                  LlvmLoweringPass.Flags flags,
                                  List<TableGenPattern> patterns,
                                  List<RegisterRef> uses,
                                  List<RegisterRef> def,
                                  IdentityHashMap<Instruction, Graph> appliedBehaviors) {
    super(behavior, inputs, outputs, flags, patterns, uses, def);
    this.appliedBehaviors = appliedBehaviors;
  }

  /**
   * Constructor which overwrites the inputs from {@code base} and {@code flags}.
   */
  public LlvmLoweringPseudoRecord(LlvmLoweringPseudoRecord base,
                                      List<TableGenInstructionOperand> inputs,
                                      LlvmLoweringPass.Flags flags) {
    super(base.behavior(), inputs, base.outputs(), flags, base.patterns(), base.uses(),
        base.defs());
    this.appliedBehaviors = base.appliedBehaviors;
  }
}
