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

import java.util.List;
import vadl.types.Type;
import vadl.viam.Operation;
import vadl.viam.graph.Node;

/**
 * An expression node representing an index of the forall-then expression. That is, a
 * bound variable restricted to some operation set.
 */
public class ForAllThenIdxNode extends ExpressionNode {

  private final List<Operation> operations;

  /**
   * The constructor.
   *
   * @param type       type of the expression.
   * @param operations operation restrictions.
   */
  public ForAllThenIdxNode(Type type, List<Operation> operations) {
    super(type);
    this.operations = operations;
  }

  public List<Operation> operations() {
    return operations;
  }

  @Override
  public ExpressionNode copy() {
    return new ForAllThenIdxNode(type(), operations);
  }

  @Override
  public Node shallowCopy() {
    return new ForAllThenIdxNode(type(), operations);
  }
}
