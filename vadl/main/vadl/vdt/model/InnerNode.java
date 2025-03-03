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

package vadl.vdt.model;

import java.util.Collection;
import vadl.vdt.utils.BitVector;

/**
 * Inner node of the decode decision tree, representing a decision to be made.
 */
public interface InnerNode extends Node {

  /**
   * Decide the next node, depending on the concrete instruction encoding provided by {@code insn}.
   *
   * @param insn The concrete encoding of an instruction to decode
   * @return The next node in the decision tree
   */
  Node decide(BitVector insn);

  /**
   * The children of this decision node.
   *
   * @return The children of this decision node
   */
  Collection<Node> children();
}
