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

package vadl.iss.passes.tcg.lowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcg.lowering.TcgEndianness;
import vadl.iss.passes.tcg.lowering.TcgExtend;
import vadl.iss.passes.tcg.lowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a memory store operation in the Tiny Code Generation (TCG) framework.
 * This class is a specific node type that encapsulates storing a value into memory.
 */
public class TcgStoreMemory extends TcgNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @DataValue
  TcgEndianness endianness;
  @Input
  TcgVRefNode addr;
  @Input
  TcgVRefNode val;

  /**
   * Constructs a TcgStoreMemory operation node which is used to store a value into memory
   * within the Tiny Code Generation (TCG) framework.
   *
   * @param size       The size of the memory to write, represented by `Tcg_8_16_32_64`.
   * @param mode       The extension mode for the value, represented by `TcgExtend`.
   * @param endianness The endianness of the operation, one of TcgEndianness values (BIG, LITTLE)
   * @param val        The value to be stored into memory, represented by `TcgV`.
   * @param addr       The address in memory where the value is to be stored, represented by `TcgV`.
   */
  public TcgStoreMemory(Tcg_8_16_32_64 size,
                        TcgExtend mode,
                        TcgEndianness endianness,
                        TcgVRefNode val,
                        TcgVRefNode addr) {
    this.size = size;
    this.extendMode = mode;
    this.endianness = endianness;
    this.addr = addr;
    this.val = val;
  }

  public Tcg_8_16_32_64 size() {
    return size;
  }

  public TcgExtend mode() {
    return extendMode;
  }

  public TcgEndianness endianness() {
    return endianness;
  }

  public TcgVRefNode addr() {
    return addr;
  }

  public TcgVRefNode val() {
    return val;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_qemu_st_" + val.width()
        + "(" + val().varName()
        + ", " + addr().varName()
        + ", 0"
        + ", " + tcgMemOp()
        + ");";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of(addr, val);
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }


  @Override
  public Node copy() {
    return new TcgStoreMemory(size, extendMode, endianness, val, addr);
  }

  @Override
  public Node shallowCopy() {
    return new TcgStoreMemory(size, extendMode, endianness, val, addr);
  }

  /**
   * Generates a memory operation string based on the size and extension mode.
   *
   * @return A string representing the memory operation with the appropriate
   *     size and extension flag.
   */
  public String tcgMemOp() {
    var first = "MO_" + size.width;
    first += switch (endianness) {
      case LITTLE -> " | MO_LE"; // if host is big endian, default is also big endian
      case BIG -> " | MO_BE";
    };
    return switch (extendMode) {
      case SIGN -> "MO_SIGN | " + first;
      case ZERO -> first; // no second flag required
    };
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(size);
    collection.add(extendMode);
    collection.add(endianness);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(addr);
    collection.add(val);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    addr = visitor.apply(this, addr, TcgVRefNode.class);
    val = visitor.apply(this, val, TcgVRefNode.class);
  }
}
