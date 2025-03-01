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
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbWriteMemNodeReplacement
    implements GraphVisitor.NodeApplier<WriteMemNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbWriteMemNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(WriteMemNode writeMemNode) {
    // LLVM has a special selection dag node when the memory
    // is written and the value truncated.
    if (writeMemNode.value() instanceof TruncateNode truncateNode) {
      var node = new LlvmTruncStore(writeMemNode, truncateNode);
      writeMemNode.replaceAndDelete(node);
    } else {
      var node = new LlvmStoreSD(Objects.requireNonNull(writeMemNode.address()),
          writeMemNode.value(),
          writeMemNode.memory(),
          writeMemNode.words());
      writeMemNode.replaceAndDelete(node);
    }

    if (writeMemNode.hasAddress()) {
      visitApplicable(writeMemNode.address());
    }
    visitApplicable(writeMemNode.value());

    return writeMemNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof WriteMemNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
