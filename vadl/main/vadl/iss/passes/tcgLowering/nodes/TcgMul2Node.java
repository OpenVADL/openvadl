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
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.opDecomposition.nodes.IssMulKind;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.NodeList;

/**
 * Represents the {@code tcg_gen_muls2}, {@code tcg_gen_mulsu2} and {@code tcg_gen_mulu2}
 * TCG operations.
 * The exact variant is determined by the {@link IssMulKind}.
 * The operation has two result destinations, one for the upper half and one for the lower
 * half of the result.
 */
public class TcgMul2Node extends TcgBinaryOpNode {

  @DataValue
  private IssMulKind kind;

  /**
   * Constructs a TCG mul2 node.
   */
  public TcgMul2Node(IssMulKind kind, TcgVRefNode lowerHalfDest, TcgVRefNode upperHalfDest,
                     TcgVRefNode arg1, TcgVRefNode arg2) {
    super(new NodeList<TcgVRefNode>(lowerHalfDest, upperHalfDest), arg1, arg2,
        lowerHalfDest.width());
    this.kind = kind;
  }

  public IssMulKind kind() {
    return kind;
  }

  public TcgVRefNode lowerHalfDest() {
    return destinations().get(0);
  }

  public TcgVRefNode upperHalfDest() {
    return destinations().get(1);
  }

  @Override
  public String tcgFunctionName() {
    var kind = kindToString();
    return "tcg_gen_mul" + kind + "2";
  }

  private String kindToString() {
    return switch (kind) {
      case SIGNED_SIGNED -> "s";
      case UNSIGNED_UNSIGNED -> "u";
      case SIGNED_UNSIGNED -> "su";
    };
  }

  @Override
  public TcgMul2Node copy() {
    return new TcgMul2Node(kind, lowerHalfDest().copy(), upperHalfDest().copy(),
        arg1.copy(), arg2.copy());
  }

  @Override
  public TcgMul2Node shallowCopy() {
    return new TcgMul2Node(kind, lowerHalfDest(), upperHalfDest(), arg1, arg2);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
