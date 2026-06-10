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
 * Represents a TCG (Tiny Code Generation) operation node responsible for loading memory.
 *
 * <p>This class encapsulates the size, extension mode, endianness, and address of the memory to
 * be loaded. It extends the TcgOpNode class, inheriting common properties of TCG operation nodes
 * like result and width.
 */
public class TcgLoadMemory extends TcgOpNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @DataValue
  TcgEndianness endianness;
  @Input
  TcgVRefNode addr;

  /**
   * Constructs a new TcgLoadMemory operation node.
   *
   * @param size       the size of the memory to be loaded, one of Tcg_8_16_32_64
   *                   values (i8, i16, i32, i64)
   * @param mode       the mode of an extension, one of TcgExtend values (SIGN, ZERO)
   * @param endianness the endianness of the operation, one of TcgEndianness values (BIG, LITTLE)
   * @param dest       the variable representing the result of the load operation
   * @param addr       the variable representing the address from where memory is to be loaded
   */
  public TcgLoadMemory(Tcg_8_16_32_64 size,
                       TcgExtend mode,
                       TcgEndianness endianness,
                       TcgVRefNode dest,
                       TcgVRefNode addr) {
    super(dest, dest.width());
    this.size = size;
    this.extendMode = mode;
    this.endianness = endianness;
    this.addr = addr;
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

  @Override
  public Set<TcgVRefNode> usedVars() {
    var used = super.usedVars();
    used.add(addr);
    return used;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_qemu_ld_" + width()
        + "(" + firstDest().varName()
        + ", " + addr().varName()
        + ", 0"
        + ", " + tcgMemOp()
        + ");";
  }

  @Override
  public Node copy() {
    return new TcgLoadMemory(size, extendMode, endianness, firstDest(), addr);
  }

  @Override
  public Node shallowCopy() {
    return new TcgLoadMemory(size, extendMode, endianness, firstDest(), addr);
  }

  /**
   * Generates the memory operation string for a TCG (Tiny Code Generation) load operation.
   *
   * <p>The method composes a memory operation string based on the size of the memory to be loaded,
   * the endianness of the memory to load from and the extension mode.
   * The format of the memory operation string varies depending on whether
   * the extension mode requires a sign extension or zero extension.
   *
   * @return A string representing the memory operation flags. It includes the memory operation
   *     size, the memory endianness flag and, if applicable, the sign extension flag.
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
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    addr = visitor.apply(this, addr, TcgVRefNode.class);
  }
}
