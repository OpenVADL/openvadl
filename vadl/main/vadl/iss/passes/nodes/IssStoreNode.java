// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.nodes;

import java.util.List;
import vadl.iss.passes.tcg.lowering.TcgEndianness;
import vadl.iss.passes.tcg.lowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.dependency.WriteMemNode;

/**
 * A lowering of the {@link WriteMemNode} node that also contains the
 * store size directly.
 */
public class IssStoreNode extends WriteMemNode {

  @DataValue
  private final Tcg_8_16_32_64 storeSize;
  @DataValue
  private final TcgEndianness endianness;

  /**
   * Constructs an IssStoreNode object which is a specialized WriteMemNode that also contains
   * information about sign extension.
   *
   * @param origin     the original read memory node
   * @param storeSize  the used TCG sized to store operation
   * @param endianness the endianness of the store operation
   */
  public IssStoreNode(WriteMemNode origin, Tcg_8_16_32_64 storeSize, TcgEndianness endianness) {
    super(origin.memory(), origin.words(), origin.address(), origin.value(),
        origin.nullableCondition());
    this.storeSize = storeSize;
    this.endianness = endianness;
  }

  public Tcg_8_16_32_64 storeSize() {
    return storeSize;
  }

  public TcgEndianness tcgEndianness() {
    return endianness;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(storeSize);
    collection.add(endianness);
  }
}
