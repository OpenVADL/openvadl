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
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameter;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterName;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.graph.Node;

/**
 * An {@link TableGenInstruction} has list of operands for inputs and outputs.
 * This class represent one element of the inputs or outputs.
 */
public class TableGenInstructionOperand {

  @Nullable
  protected final Node origin;

  private final TableGenParameter parameter;

  public TableGenInstructionOperand(@Nullable Node origin, TableGenParameter parameter) {
    this.parameter = parameter;
    this.origin = origin;
  }

  public TableGenInstructionOperand(@Nullable Node origin, String type, String name) {
    this(origin, new TableGenParameterTypeAndName(type, name));
  }

  public TableGenInstructionOperand(@Nullable Node origin, String name) {
    this(origin, new TableGenParameterName(name));
  }

  public String render() {
    return parameter.render();
  }

  public TableGenParameter parameter() {
    return parameter;
  }

  @Nullable
  public Node origin() {
    return origin;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TableGenInstructionOperand x) {
      return x.parameter.equals(this.parameter);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameter);
  }
}
