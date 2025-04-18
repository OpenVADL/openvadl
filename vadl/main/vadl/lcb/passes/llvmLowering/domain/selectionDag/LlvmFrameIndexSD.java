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

package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;

/**
 * LLVM node which represents the frame index as selection dag node.
 */
public class LlvmFrameIndexSD extends ReadRegFileNode implements LlvmNodeLowerable {
  public static final String NAME = "AddrFI";

  public LlvmFrameIndexSD(ReadRegFileNode obj) {
    this(obj.registerFile(), obj.address(), obj.type(), obj.staticCounterAccess());
  }

  private LlvmFrameIndexSD(RegisterFile registerFile, ExpressionNode address, DataType type,
                           @Nullable Counter staticCounterAccess) {
    super(registerFile, address, type, staticCounterAccess);
  }


  @Override
  public LlvmFrameIndexSD copy() {
    return new LlvmFrameIndexSD(registerFile(), address().copy(), type(),
        staticCounterAccess());
  }

  @Override
  public LlvmFrameIndexSD shallowCopy() {
    return new LlvmFrameIndexSD(registerFile(), address(), type(), staticCounterAccess());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    }
    if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    }
  }

  @Override
  public String lower() {
    return NAME;
  }
}
