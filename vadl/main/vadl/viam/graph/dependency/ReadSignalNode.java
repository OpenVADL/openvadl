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
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

public class ReadSignalNode extends ReadResourceNode {

  @DataValue
  protected Signal signal;

  public ReadSignalNode(Signal signal) {
    super((ExpressionNode) null, signal.resultType());
    this.signal = signal;
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

  @Override
  public ExpressionNode copy() {
    return new ReadSignalNode(signal);
  }

  @Override
  public Node shallowCopy() {
    return new ReadSignalNode(signal);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
