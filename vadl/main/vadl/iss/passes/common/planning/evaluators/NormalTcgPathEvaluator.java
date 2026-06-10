// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common.planning.evaluators;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.Instruction;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * Checks whether an instruction can stay on the shared non-helper path without any dedicated
 * vector-region optimization.
 */
public final class NormalTcgPathEvaluator {

  /**
   * Returns whether the instruction can remain on the shared non-helper path without a dedicated
   * vector-region rewrite.
   */
  public boolean isViable(Instruction instruction) {
    var behavior = instruction.behavior();

    // This is the baseline shared non-helper path before any vector-specific region rewriting.
    // It stays valid only while the graph does not require vector/tensor semantics that the
    // default lowering still does not model directly.
    var hasCpuVectorReads = behavior.getNodes(IssReadRegNode.class)
        .anyMatch(node -> regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasCpuVectorWrites = behavior.getNodes(IssWriteRegNode.class)
        .anyMatch(node -> regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasForall = behavior.getNodes(ForallNode.class).findAny().isPresent();
    var hasTensor = behavior.getNodes(TensorNode.class).findAny().isPresent();
    var hasFold = behavior.getNodes(FoldNode.class).findAny().isPresent();

    return !(hasCpuVectorReads || hasCpuVectorWrites || hasForall || hasTensor || hasFold);
  }
}
