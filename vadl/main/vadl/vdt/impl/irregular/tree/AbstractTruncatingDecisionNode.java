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

import vadl.vdt.model.InnerNode;

/**
 * A decision node which only uses the bits from a given offset up to a given length.
 */
public abstract class AbstractTruncatingDecisionNode implements InnerNode {

  private final int offset;
  private final int length;

  /**
   * Creates a new truncating decision node.
   *
   * @param offset The offset of bits to skip prior to matching
   * @param length The number of bits to match
   */
  public AbstractTruncatingDecisionNode(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }
}
