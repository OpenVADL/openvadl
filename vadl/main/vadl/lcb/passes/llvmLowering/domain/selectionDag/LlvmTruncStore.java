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

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.LlvmMayStoreMemory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.LlvmSideEffectPatternIncluded;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This is a special LLVM node which is a combination of
 * {@link WriteMemNode} and {@link vadl.viam.graph.dependency.TruncateNode}.
 */
public class LlvmTruncStore extends WriteResourceNode implements LlvmNodeLowerable,
    LlvmSideEffectPatternIncluded, LlvmMayStoreMemory {

  @DataValue
  protected Memory memory;

  @DataValue
  protected int words;

  @DataValue
  protected Type truncatedType;

  /**
   * Constructor which should be only used by {@link ReplaceWithLlvmSDNodesVisitor}.
   */
  public LlvmTruncStore(WriteMemNode writeMemNode,
                        TruncateNode value) {
    this(writeMemNode.memory(),
        writeMemNode.words(),
        value.type(),
        writeMemNode.address(),
        value.value());
  }

  private LlvmTruncStore(Memory memory,
                         int words,
                         Type truncatedType,
                         @Nullable ExpressionNode address,
                         ExpressionNode value) {
    super(address, value);
    this.memory = memory;
    this.words = words;
    this.truncatedType = truncatedType;
  }

  @Override
  public Node copy() {
    return new LlvmTruncStore(memory, words, truncatedType,
        address().copy(),
        value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmTruncStore(memory, words, truncatedType, address(), value);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Override
  public Resource resourceDefinition() {
    return memory;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
    collection.add(words);
    collection.add(truncatedType);
  }

  @Override
  public String lower() {
    ensure(memory.wordSize() == 8, "Memory word size must be 8 because "
        + "LLVM requires it");
    return "truncstorei" + words * memory.wordSize();
  }
}
