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

package vadl.viam.graph.dependency;

import java.util.List;
import javax.annotation.Nullable;
import vadl.types.DataType;
import vadl.viam.Counter;
import vadl.viam.Register;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.NodeList;

/**
 * The ReadRegNode class is a subclass of ReadNode that represents
 * a node that reads a value from a register location.
 */
// TODO: Remove once all generator adapted ReadRegTensorNode
public class ReadRegNode extends ReadRegTensorNode {


  /**
   * Reads a value from a register.
   *
   * @param register            the register to read from
   * @param type                the data type of the value to be read
   * @param staticCounterAccess the {@link Counter} that is read,
   *                            or null if no counter is read
   */
  public ReadRegNode(Register register, DataType type,
                     @Nullable Counter staticCounterAccess) {
    super(register, new NodeList<>(), type, staticCounterAccess);
  }

  public Register register() {
    return (Register) super.resourceDefinition();
  }

  
  @Override
  public boolean hasAddress() {
    return false;
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
  }

  @Override
  public ReadRegNode copy() {
    return new ReadRegNode(register(), type(), staticCounterAccess());
  }

  @Override
  public ReadRegNode shallowCopy() {
    return copy();
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(register().simpleName());
  }
}
