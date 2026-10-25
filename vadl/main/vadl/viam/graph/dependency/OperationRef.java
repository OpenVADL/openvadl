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

package vadl.viam.graph.dependency;

import vadl.types.OperationType;
import vadl.types.Type;
import vadl.viam.Operation;
import vadl.viam.graph.Node;

/**
 * Reference to an {@link Operation} definition.
 */
public class OperationRef extends ExpressionNode {

  private final Operation operation;

  /**
   * Creates a new {@link OperationRef} to the given {@link Operation}.
   *
   * @param type      The type of the given operation..
   * @param operation The {@link Operation} to reference.
   */
  public OperationRef(OperationType type, Operation operation) {
    super(type);

    this.operation = operation;
  }

  public Operation operation() {
    return operation;
  }

  @Override
  public ExpressionNode copy() {
    return new OperationRef((OperationType) type(), operation);
  }

  @Override
  public Node shallowCopy() {
    return new OperationRef((OperationType) type(), operation);
  }

}
