// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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
import vadl.javaannotations.viam.DataValue;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a read in an expression tree from a register, whose value is known at
 * TCG generation time, because it is part of the TC state. Generating a read
 * instruction is thus not necessary.
 */
public class ReadStaticRegTensorNode extends ExpressionNode {

  @DataValue
  protected RegisterTensor regTensor;

  public ReadStaticRegTensorNode(RegisterTensor regTensor) {
    super(regTensor.resultType());
    this.regTensor = regTensor;
  }

  @Override
  public ExpressionNode copy() {
    return new ReadStaticRegTensorNode(regTensor);
  }

  @Override
  public Node shallowCopy() {
    return new ReadStaticRegTensorNode(regTensor);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(regTensor);
  }

  public RegisterTensor regTensor() {
    return regTensor;
  }
}
