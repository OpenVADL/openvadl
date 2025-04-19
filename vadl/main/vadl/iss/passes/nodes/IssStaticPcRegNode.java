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
import vadl.javaannotations.viam.DataValue;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a static program counter (PC) register in an expression tree.
 * In the TCG generation context, we can get the current PC from {@code ctx->pc_curr},
 * so a read to the PC cpu register (TCGv) is not necessary.
 */
public class IssStaticPcRegNode extends ExpressionNode {

  @DataValue
  private final RegisterTensor pc;

  public IssStaticPcRegNode(RegisterTensor pc) {
    super(pc.resultType());
    this.pc = pc;
  }

  public RegisterTensor register() {
    return pc;
  }

  @Override
  public ExpressionNode copy() {
    return new IssStaticPcRegNode(pc);
  }

  @Override
  public Node shallowCopy() {
    return new IssStaticPcRegNode(pc);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(pc);
  }
}
