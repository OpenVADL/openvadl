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

package vadl.iss.passes.opDecomposition.decomposer;

import java.util.IdentityHashMap;
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;

/**
 * Utility for instantiating forall bodies by replacing the forall-index node with a constant.
 */
public final class ForallSubstitution {

  private ForallSubstitution() {
  }

  public static ExpressionNode copyWithIndexSubstitution(ExpressionNode root,
                                                         ForIdxNode idx,
                                                         int idxValue) {
    var idxConst = Constant.Value.of(idxValue, idx.type()).toNode();
    return copyWithNodeSubstitution(root, idx, idxConst, new IdentityHashMap<>());
  }

  private static ExpressionNode copyWithNodeSubstitution(ExpressionNode node,
                                                         ForIdxNode toReplace,
                                                         ExpressionNode replacement,
                                                         IdentityHashMap<ExpressionNode,
                                                             ExpressionNode> cache) {
    if (matchesForallIdx(node, toReplace)) {
      return replacement;
    }

    var cached = cache.get(node);
    if (cached != null) {
      return cached;
    }

    var copy = (ExpressionNode) node.shallowCopy();
    cache.put(node, copy);
    copy.applyOnInputs((self, input) -> {
      if (input instanceof ExpressionNode inputExpr) {
        return copyWithNodeSubstitution(inputExpr, toReplace, replacement, cache);
      }
      return input;
    });

    if (copy instanceof Canonicalizable canonicalizable) {
      var canonical = canonicalizable.canonical();
      if (canonical instanceof ExpressionNode exprCanonical) {
        copy = exprCanonical;
      }
    }
    cache.put(node, copy);
    return copy;
  }

  private static boolean matchesForallIdx(ExpressionNode node, ForIdxNode idx) {
    if (node == idx) {
      return true;
    }
    if (!(node instanceof ForIdxNode other)) {
      return false;
    }
    return other.fromIdx() == idx.fromIdx()
        && other.toIdx() == idx.toIdx()
        && other.type().equals(idx.type());
  }
}
