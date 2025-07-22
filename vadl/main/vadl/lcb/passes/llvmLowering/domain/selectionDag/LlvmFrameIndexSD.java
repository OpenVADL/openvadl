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

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * LLVM node which represents the frame index as selection dag node.
 */
public class LlvmFrameIndexSD extends ReadResourceNode implements LlvmNodeLowerable {
  public static final String NAME = "AddrFI";
  private final Resource resource;
  private final Node origin;

  public LlvmFrameIndexSD(ReadRegTensorNode obj) {
    this(obj.indices(), obj.type(), obj.resourceDefinition(), obj);
    obj.regTensor().ensure(obj.regTensor().isRegisterFile(), "must be register file");
  }

  public LlvmFrameIndexSD(ReadArtificialResNode obj) {
    this(obj.indices(), obj.type(), obj.resourceDefinition(), obj);
    obj.registerTensor().ensure(obj.registerTensor().isRegisterFile(), "must be register file");
  }

  private LlvmFrameIndexSD(NodeList<ExpressionNode> indices,
                           DataType type,
                           Resource resource,
                           Node origin) {
    super(indices, type);
    this.resource = resource;
    this.origin = origin;
  }

  @Override
  public LlvmFrameIndexSD copy() {
    return new LlvmFrameIndexSD(new NodeList<>(indices.stream().map(ExpressionNode::copy).toList()),
        type(), resource, origin.copy());
  }

  @Override
  public LlvmFrameIndexSD shallowCopy() {
    return new LlvmFrameIndexSD(indices, type(), resource, origin);
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

  @Override
  public Resource resourceDefinition() {
    return resource;
  }

  public Node origin() {
    return origin;
  }
}
