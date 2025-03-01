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
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbZeroExtendNodeReplacement
    implements GraphVisitor.NodeApplier<ZeroExtendNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbZeroExtendNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(ZeroExtendNode node) {
    if (node.value() instanceof ReadMemNode readMemNode) {
      // Merge SignExtend and ReadMem to LlvmZExtLoad
      node.replaceAndDelete(
          new LlvmTypeCastSD(new LlvmZExtLoad(readMemNode), makeSigned(node.type())));
      visitApplicable(readMemNode.address());
    } else {
      // Remove all nodes
      for (var usage : node.usages().toList()) {
        usage.replaceInput(node, node.value());
      }
      visitApplicable(node.value());
    }
    return node;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ZeroExtendNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }

  private Type makeSigned(DataType type) {
    if (!type.isSigned()) {
      if (type instanceof BitsType bitsType) {
        return SIntType.bits(bitsType.bitWidth());
      }
    }

    return type;
  }
}
