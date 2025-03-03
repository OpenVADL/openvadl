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

package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.viam.graph.Graph;

/**
 * TableGen pattern has a tree for LLVM Dag nodes to select a pattern in the instruction
 * selection. This is represented by {@code selector}.
 * And a tree for the emitted machine instruction. This is represented by {@code machine}.
 */
public abstract class TableGenPattern {
  protected final Graph selector;

  protected TableGenPattern(Graph selector) {
    this.selector = selector;
  }

  /**
   * Checks whether the {@code selector} is a valid TableGen pattern.
   *
   * @return true if ok.
   */
  public boolean isPatternLowerable() {
    return selector.getDataflowRoots().stream().allMatch(node ->
        node instanceof LlvmNodeLowerable
            || node instanceof LcbMachineInstructionNode);
  }

  public Graph selector() {
    return selector;
  }

  /**
   * Copy the tablegen pattern.
   */
  public abstract TableGenPattern copy();
}
