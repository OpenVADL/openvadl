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

package vadl.vdt.impl.regular;

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
 * Simple implementation of an inner node, holding a mask, a fallback node and a map of children.
 * A decision is based on the bits specified by the mask and depending on the matching child bit
 * pattern. If no child matches, the fallback node is returned (if present).
 */
public class InnerNodeImpl implements InnerNode {

  private final BitVector mask;
  private final @Nullable Node fallback;
  private final Map<BitPattern, Node> children;

  /**
   * Creates a new inner node.
   *
   * @param mask     the mask specifying the bits to consider
   * @param children the children to match against
   * @param fallback the fallback node to return if no child matches
   */
  public InnerNodeImpl(BitVector mask, Map<BitPattern, Node> children, @Nullable Node fallback) {
    this.mask = mask;
    this.children = children;
    this.fallback = fallback;
  }

  @Override
  public Node decide(BitVector insn) {

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      if (entry.getKey().test(insn)) {
        return entry.getValue();
      }
    }

    if (fallback != null) {
      return fallback;
    }

    throw new RuntimeException("No decision found for " + insn);
  }

  public BitVector getMask() {
    return mask;
  }

  public @Nullable Node getFallback() {
    return fallback;
  }

  public Map<BitPattern, Node> getChildren() {
    return children;
  }

  @Override
  public Collection<Node> children() {
    var result = new HashSet<>(children.values());
    if (fallback != null) {
      result.add(fallback);
    }
    return result;
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
