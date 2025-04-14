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

package vadl.vdt.target.common;

import java.nio.ByteOrder;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.target.common.dto.DecodedInstruction;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;
import vadl.viam.Constant.Value;

/**
 * A decoder that uses the in-memory decision tree to decode instructions. Useful for testing and
 * debugging.
 */
public class DecisionTreeDecoder implements Visitor<Instruction> {

  private final Node decisionTree;
  private @Nullable BitVector encoding;

  public DecisionTreeDecoder(Node tree) {
    this.decisionTree = tree;
  }

  public Instruction decide(BitVector insn) {
    this.encoding = insn;
    return Objects.requireNonNull(decisionTree.accept(this));
  }

  /**
   * Decode an instruction encoded in the given byte order.
   *
   * @param encoding The encoded instruction
   * @return The decoded instruction.
   */
  public DecodedInstruction decode(Value encoding, ByteOrder byteOrder) {
    this.encoding = BitVector.fromValue(encoding.integer(), encoding.type().bitWidth());
    var insn = Objects.requireNonNull(decisionTree.accept(this));
    return new DecodedInstruction(insn, encoding.integer(), byteOrder);
  }

  @Override
  public Instruction visit(InnerNode node) {
    final BitVector encoding = Objects.requireNonNull(this.encoding);
    final Node decide = node.decide(encoding);
    if (decide == null) {
      throw new IllegalArgumentException("No decision found for " + encoding);
    }
    return Objects.requireNonNull(decide.accept(this));
  }

  @Override
  public Instruction visit(LeafNode node) {
    return node.instruction();
  }
}
