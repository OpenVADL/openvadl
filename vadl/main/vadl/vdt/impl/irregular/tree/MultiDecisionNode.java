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

package vadl.vdt.impl.irregular.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Simple implementation of an inner node, deciding the matching child node based on pattern
 * matching.
 */
public class MultiDecisionNode implements InnerNode {

  private final BitVector mask;
  private final Map<BitPattern, Node> children;

  /**
   * Creates a new inner node.
   *
   * @param children The children to match against
   */
  public MultiDecisionNode(BitVector mask, Map<BitPattern, Node> children) {
    this.mask = mask;
    this.children = children;
  }

  @Override
  public Node decide(BitVector insn) {

    // extend/truncate the instruction to the relevant bits before testing
    final BitVector i = insn
        .rightPad(mask.width(), false);

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      if (entry.getKey().test(i)) {
        return entry.getValue();
      }
    }

    throw new RuntimeException("No decision found for " + insn);
  }

  public BitVector getMask() {
    return mask;
  }

  public Map<BitPattern, Node> getChildren() {
    return children;
  }

  @Override
  public Collection<Node> children() {
    return new HashSet<>(children.values());
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
