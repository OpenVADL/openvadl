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

package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Extract a bitfield from t1, placing the result in dest().
 *
 * <p>The bitfield is described by pos/len, which are immediate values, as above for deposit.
 * For extract_*, the result will be extended to the left with zeros; for sextract_*,
 * the result will be extended to the left with copies of the bitfield sign bit at pos + len - 1.
 *
 * <p>For example, “sextract_i32 dest, t1, 8, 4” indicates a 4-bit field at bit 8.
 * This operation would be equivalent to
 * {@code dest = (t1 << 20) >> 28} (using an arithmetic right shift).
 */
public class TcgExtractNode extends TcgUnaryOpNode {

  @DataValue
  private final int offset;

  @DataValue
  private final int len;

  @DataValue
  private final TcgExtend extendMode;

  /**
   * Construct a TCG extract node.
   *
   * @param dest   of result
   * @param t1     source variable
   * @param offset offset (from lsb) where to start extraction
   * @param len    of extraction (from lsb)
   */
  public TcgExtractNode(TcgVRefNode dest,
                        TcgVRefNode t1, int offset, int len, TcgExtend extendMode) {
    super(dest, t1);
    this.offset = offset;
    this.len = len;
    this.extendMode = extendMode;
  }

  public int pos() {
    return offset;
  }

  public int len() {
    return len;
  }

  public TcgExtend signed() {
    return extendMode;
  }

  @Override
  public String tcgFunctionName() {
    var sign = extendMode == TcgExtend.SIGN ? "s" : "";
    return "tcg_gen_" + sign + "extract_" + width();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg.varName() + ", " + offset
        + ", "
        + len + ");";
  }

  @Override
  public TcgExtractNode copy() {
    return new TcgExtractNode(firstDest().copy(), arg.copy(), offset, len, extendMode);
  }

  @Override
  public Node shallowCopy() {
    return new TcgExtractNode(firstDest(), arg, offset, len, extendMode);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(offset);
    collection.add(len);
    collection.add(extendMode);
  }
}
