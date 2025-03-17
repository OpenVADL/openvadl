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

package vadl.iss.passes.opDecomposition.nodes;

import vadl.types.Type;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The base class of all ISS (only) related expression nodes.
 * Those nodes are only used by the ISS and are required for intermediate lowering
 * for optimization purposes.
 */
public abstract class IssExprNode extends ExpressionNode {
  public IssExprNode(Type type) {
    super(type);
  }

}
