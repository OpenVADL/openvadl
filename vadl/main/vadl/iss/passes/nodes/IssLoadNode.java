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

package vadl.iss.passes.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.graph.dependency.ReadMemNode;

/**
 * A lowering of the {@link ReadMemNode} that holds the tcg extend mode as well as
 * the load size.
 */
public class IssLoadNode extends ReadMemNode {

  @DataValue
  private final TcgExtend tcgExtend;
  @DataValue
  private final Tcg_8_16_32_64 loadSize;

  /**
   * Constructs an IssLoadNode object which is a specialized ReadMemNode that also contains
   * information about sign extension.
   *
   * @param origin    the original read memory node
   * @param tcgExtend the extension information (sign or unsigned)
   * @param type      the data type of the value being read
   */
  public IssLoadNode(ReadMemNode origin, TcgExtend tcgExtend, Tcg_8_16_32_64 loadSize,
                     DataType type) {
    super(origin.memory(), origin.words(), origin.address(), type);
    this.tcgExtend = tcgExtend;
    this.loadSize = loadSize;
  }

  public Tcg_8_16_32_64 loadSize() {
    return loadSize;
  }

  public TcgExtend tcgExtend() {
    return tcgExtend;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(tcgExtend);
    collection.add(loadSize);
  }
}
