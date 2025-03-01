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


import java.util.List;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Format;
import vadl.viam.graph.Graph;

/**
 * Contains information for the lowering of instructions.
 */
public class LlvmLoweringRecord {
  private final Graph behavior;
  private final List<TableGenInstructionOperand> inputs;
  private final List<TableGenInstructionOperand> outputs;
  private final LlvmLoweringPass.Flags flags;
  private final List<TableGenPattern> patterns;
  private final List<RegisterRef> uses;
  private final List<RegisterRef> def;

  /**
   * Constructor.
   */
  public LlvmLoweringRecord(Graph behavior, List<TableGenInstructionOperand> inputs,
                            List<TableGenInstructionOperand> outputs, LlvmLoweringPass.Flags flags,
                            List<TableGenPattern> patterns, List<RegisterRef> uses,
                            List<RegisterRef> def) {
    this.behavior = behavior;
    this.inputs = inputs;
    this.outputs = outputs;
    this.flags = flags;
    this.patterns = patterns;
    this.uses = uses;
    this.def = def;
  }

  public Graph behavior() {
    return behavior;
  }

  public List<TableGenInstructionOperand> inputs() {
    return inputs;
  }

  public List<TableGenInstructionOperand> outputs() {
    return outputs;
  }

  public LlvmLoweringPass.Flags flags() {
    return flags;
  }

  public List<TableGenPattern> patterns() {
    return patterns;
  }

  public List<RegisterRef> uses() {
    return uses;
  }

  public List<RegisterRef> defs() {
    return def;
  }

  /**
   * Find the index in the {@link #inputs} by the given field.
   */
  public int findInputIndex(Format.Field field) {
    for (int i = 0; i < inputs.size(); i++) {
      if (inputs.get(i) instanceof ReferencesFormatField x && x.formatField().equals(field)) {
        return i;
      }
    }

    throw Diagnostic.error("Cannot find field in inputs.", field.sourceLocation()).build();
  }
}
