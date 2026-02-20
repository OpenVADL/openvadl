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
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Unified ISS register write node for base and alias accesses.
 */
public class IssWriteRegNode extends WriteRegTensorNode {

  public enum AccessKind {
    BASE,
    ALIAS
  }

  public enum WriteGuardKind {
    NONE,
    ZERO_CONSTRAINT,
    CONDITIONAL
  }

  @DataValue
  private final AccessKind accessKind;
  @DataValue
  private final WriteGuardKind writeGuardKind;
  @DataValue
  @Nullable
  private final String accessorName;
  @DataValue
  private final NodeList<ExpressionNode> accessorIndices;

  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable ExpressionNode condition) {
    this(regTensor, resourceIndices, value, condition, AccessKind.BASE, WriteGuardKind.NONE, null,
        resourceIndices.copy());
  }

  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable ExpressionNode condition,
                         AccessKind accessKind,
                         WriteGuardKind writeGuardKind,
                         @Nullable String accessorName,
                         NodeList<ExpressionNode> accessorIndices) {
    this(regTensor, resourceIndices, value, null, condition, accessKind, writeGuardKind,
        accessorName, accessorIndices);
  }

  public IssWriteRegNode(RegisterTensor regTensor,
                         NodeList<ExpressionNode> resourceIndices,
                         ExpressionNode value,
                         @Nullable Counter staticCounterAccess,
                         @Nullable ExpressionNode condition,
                         AccessKind accessKind,
                         WriteGuardKind writeGuardKind,
                         @Nullable String accessorName,
                         NodeList<ExpressionNode> accessorIndices) {
    super(regTensor, resourceIndices, value, staticCounterAccess, condition);
    this.accessKind = accessKind;
    this.writeGuardKind = writeGuardKind;
    this.accessorName = accessorName;
    this.accessorIndices = accessorIndices;
  }

  public AccessKind accessKind() {
    return accessKind;
  }

  public WriteGuardKind writeGuardKind() {
    return writeGuardKind;
  }

  public @Nullable String accessorName() {
    return accessorName;
  }

  public NodeList<ExpressionNode> accessorIndices() {
    return accessorIndices;
  }

  @Override
  public Node copy() {
    return new IssWriteRegNode(
        regTensor(),
        indices().copy(),
        value().copy(),
        staticCounterAccess(),
        nullableCondition() == null ? null : nullableCondition().copy(),
        accessKind,
        writeGuardKind,
        accessorName,
        accessorIndices.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new IssWriteRegNode(
        regTensor(),
        indices(),
        value(),
        staticCounterAccess(),
        nullableCondition(),
        accessKind,
        writeGuardKind,
        accessorName,
        accessorIndices
    );
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(accessKind);
    collection.add(writeGuardKind);
    collection.add(accessorName);
    collection.addAll(accessorIndices);
  }
}
