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

import vadl.types.Type;
import vadl.viam.Memory;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a static read of the bi-endianness condition value from
 * the {@code DisasContext} like this: {@code ctx->endian_cond}. The
 * condition determines whether memory ops are big- or little-endian.
 * The actual condition (from {@link Memory#biEndianCondition()}) is
 * evaluated once per TB during the creation of the {@code DisasContext}.
 */
public class IssStaticEndianConditionNode extends ExpressionNode {

  public IssStaticEndianConditionNode() {
    super(Type.bool());
  }

  @Override
  public ExpressionNode copy() {
    return new IssStaticEndianConditionNode();
  }

  @Override
  public Node shallowCopy() {
    return new IssStaticEndianConditionNode();
  }
}
