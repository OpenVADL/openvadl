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
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Representing writing/driving the value of a signal. Any single signal can only have one driver.
 */
public class WriteSignalNode extends WriteResourceNode {

  @DataValue
  protected Signal signal;

  public WriteSignalNode(Signal signal, ExpressionNode value) {
    this(signal, value, null);
  }

  public WriteSignalNode(Signal signal, ExpressionNode value, @Nullable ExpressionNode condition) {
    super(new NodeList<>(), value, condition);
    this.signal = signal;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(condition == null
            || (condition instanceof ConstantNode c && c.constant().asVal().bool()),
        "Write signal condition must be null or true");
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(signal);
  }

  @Override
  public Resource resourceDefinition() {
    return signal;
  }

  public Signal signal() {
    return signal;
  }

  @Override
  public Node copy() {
    return new WriteSignalNode(signal, value.copy(), (condition != null) ? condition.copy() : null);
  }

  @Override
  public Node shallowCopy() {
    return new WriteSignalNode(signal, value, condition);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
