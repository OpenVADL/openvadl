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
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * LLVM node which represents the frame index as selection dag node.
 */
public class LlvmFrameIndexSD extends ReadRegTensorNode implements LlvmNodeLowerable {
  public static final String NAME = "AddrFI";

  public LlvmFrameIndexSD(ReadRegTensorNode obj) {
    this(obj.regTensor(), obj.indices(), obj.type(), obj.staticCounterAccess());
    obj.regTensor().ensure(obj.regTensor().isRegisterFile(), "must be register file");
  }

  private LlvmFrameIndexSD(RegisterTensor registerFile, NodeList<ExpressionNode> addresses,
                           DataType type,
                           @Nullable Counter staticCounterAccess) {
    super(registerFile, addresses, type, staticCounterAccess);
  }


  @Override
  public LlvmFrameIndexSD copy() {
    return new LlvmFrameIndexSD(regTensor, indices.copy(), type(),
        staticCounterAccess());
  }

  @Override
  public LlvmFrameIndexSD shallowCopy() {
    return new LlvmFrameIndexSD(regTensor, indices, type(), staticCounterAccess());
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
