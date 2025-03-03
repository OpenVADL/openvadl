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

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.LlvmMayLoadMemory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.DataType;
import vadl.viam.Memory;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadMemNode;

/**
 * LLVM node for a memory load zero extend.
 */
public class LlvmZExtLoad extends ReadMemNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayLoadMemory {

  public LlvmZExtLoad(ReadMemNode readMemNode) {
    this(readMemNode.address(), readMemNode.memory(), readMemNode.words());
  }

  public LlvmZExtLoad(ExpressionNode address,
                      Memory memory,
                      int words) {
    super(memory, words, address, (DataType) address.type());
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmZExtLoad((ExpressionNode) Objects.requireNonNull(address).copy(),
        memory,
        words);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmZExtLoad(Objects.requireNonNull(address), memory, words);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Override
  public String lower() {
    ensure(memory.wordSize() == 8, "Memory word size must be 8 because "
        + "LLVM requires it");
    return "zextloadi" + words * memory.wordSize();
  }
}
