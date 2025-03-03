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
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Class representing a truncation operation in the TCG (Tiny Code Generator).
 * This node performs a bit-width truncation on a single operand.
 * Extends from TcgUnaryOpNode to utilize unary operation functionality.
 */
public class TcgTruncateNode extends TcgUnaryOpNode {

  @DataValue
  int bitWidth;

  public TcgTruncateNode(TcgVRefNode res, TcgVRefNode arg, int bitWidth) {
    super(res, arg);
    this.bitWidth = bitWidth;
  }

  public int bitWidth() {
    return bitWidth;
  }

  @Override
  public String tcgFunctionName() {
    return "gen_trunc";
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "("
        + firstDest().varName()
        + ", " + arg.varName()
        + ", " + bitWidth
        + ");";
  }

  @Override
  public Node copy() {
    return new TcgTruncateNode(firstDest().copy(TcgVRefNode.class), arg.copy(TcgVRefNode.class),
        bitWidth);
  }

  @Override
  public Node shallowCopy() {
    return new TcgTruncateNode(firstDest(), arg, bitWidth);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(bitWidth);
  }
}
