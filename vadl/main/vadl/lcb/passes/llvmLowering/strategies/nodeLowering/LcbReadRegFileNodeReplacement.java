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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbReadRegFileNodeReplacement
    implements GraphVisitor.NodeApplier<ReadRegTensorNode, Node> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbReadRegFileNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public Node visit(ReadRegTensorNode readRegFileNode) {
    // If the address is constant and register file has a constraint for, then we should replace it
    // by the constraint value.

    readRegFileNode.regTensor()
        .ensure(readRegFileNode.regTensor().isRegisterFile(), "must be register file");

    if (readRegFileNode.hasConstantAddress()) {
      var address = (ConstantNode) readRegFileNode.address();
      var constraint = Arrays.stream(readRegFileNode.regTensor().constraints())
          .filter(c -> c.indices().getFirst().equals(address.constant()))
          .findFirst();

      if (constraint.isPresent()) {
        return new ConstantNode(constraint.get().value());
      } else {
        DeferredDiagnosticStore.add(Diagnostic.warning(
            "Reading from a register file with constant index but the register has no "
                + "constraint value.",
            address.sourceLocation()).build());
        return readRegFileNode;
      }
    } else {
      visitApplicable(readRegFileNode.address());

      return new LlvmReadRegFileNode(readRegFileNode.regTensor(), readRegFileNode.address(),
          readRegFileNode.type(), readRegFileNode.staticCounterAccess());
    }
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ReadRegTensorNode readRegTensorNode
        && readRegTensorNode.regTensor().isRegisterFile();
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
