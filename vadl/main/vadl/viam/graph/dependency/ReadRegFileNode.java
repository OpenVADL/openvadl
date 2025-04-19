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
import vadl.viam.RegisterFile;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.NodeList;

/**
 * A read of a register file in the behaviour graph. It takes one expression node as input
 * that represents the address/index value.
 */
// TODO: Remove once all generator adapted ReadRegTensorNode
public class ReadRegFileNode extends ReadRegTensorNode implements HasRegisterFile {

  /**
   * Constructs the node, which represents a read from a register file at some specific index.
   *
   * @param registerFile        the register-file definition to be read from
   * @param address             the index of the specific register in the register-file
   * @param type                the type this node should be result in
   * @param staticCounterAccess the {@link Counter} this node reads from, or null if
   *                            it is not known
   */
  public ReadRegFileNode(RegisterFile registerFile, ExpressionNode address,
                         DataType type, @Nullable Counter staticCounterAccess) {
    super(registerFile, new NodeList<>(address), type, staticCounterAccess);
  }

  @Override
  public RegisterFile registerFile() {
    return (RegisterFile) resourceDefinition();
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
  }

  @Override
  public ReadRegFileNode copy() {
    return new ReadRegFileNode(registerFile(), address().copy(), type(), staticCounterAccess());
  }

  @Override
  public ReadRegFileNode shallowCopy() {
    return new ReadRegFileNode(registerFile(), address(), type(), staticCounterAccess());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(registerFile().simpleName())
        .append("(");
    address().prettyPrint(sb);
    sb.append(")");
  }
}
