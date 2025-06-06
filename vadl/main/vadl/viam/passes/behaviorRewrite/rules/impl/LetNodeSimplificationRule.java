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

package vadl.viam.passes.behaviorRewrite.rules.impl;

import java.util.Optional;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.passes.behaviorRewrite.rules.BehaviorRewriteSimplificationRule;

/**
 * Simplification rule for a graph which let nodes.
 */
public class LetNodeSimplificationRule
    implements BehaviorRewriteSimplificationRule {
  @Override
  public Optional<Node> simplify(Node node) {
    if (node instanceof LetNode n) {
      return Optional.of(n.expression());
    }

    return Optional.empty();
  }
}
