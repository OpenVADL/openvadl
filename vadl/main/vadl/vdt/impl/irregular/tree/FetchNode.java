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
import java.util.List;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitVector;

/**
 * Inner node indicating that the next/remaining part of the instruction word should be fetched
 * in order to continue with the decoding process.
 */
public class FetchNode implements InnerNode {

  private final int fetchSize;
  private final Node child;

  /**
   * Creates a new fetch node.
   *
   * @param fetchSize the number of bits to fetch
   * @param child     the child to continue decoding with
   */
  public FetchNode(int fetchSize, Node child) {
    this.fetchSize = fetchSize;
    this.child = child;
  }

  /**
   * The number of bits to fetch.
   *
   * @return the number of bits
   */
  public int getFetchSize() {
    return fetchSize;
  }

  @Override
  public Node decide(BitVector insn) {
    return child;
  }

  @Override
  public Collection<Node> children() {
    return List.of(child);
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
