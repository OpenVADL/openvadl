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

package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * While {@link LcbFieldAccessRefNodeReplacement} converts every
 * {@link FieldAccessRefNode} into {@link LlvmFieldAccessRefNode},
 * this class converts it into {@link LlvmBasicBlockSD}. This means that the field should be
 * treated like an immediate, but it is a basic block.
 */
public class LcbFieldAccessRefNodeByLlvmBasicBlockReplacement
    implements GraphVisitor.NodeApplier<FieldAccessRefNode, LlvmBasicBlockSD> {
  private final PrintableInstruction instruction;
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;
  private final ValueType architectureType;

  /**
   * Constructor.
   */
  public LcbFieldAccessRefNodeByLlvmBasicBlockReplacement(
      PrintableInstruction instruction,
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer,
      ValueType architectureType) {
    this.instruction = instruction;
    this.replacer = replacer;
    this.architectureType = architectureType;
  }

  @Nullable
  @Override
  public LlvmBasicBlockSD visit(FieldAccessRefNode fieldAccessRefNode) {
    var originalType = fieldAccessRefNode.fieldAccess().accessFunction().returnType();

    return new LlvmBasicBlockSD(instruction,
        fieldAccessRefNode.fieldAccess(),
        originalType,
        architectureType);
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof FieldAccessRefNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
