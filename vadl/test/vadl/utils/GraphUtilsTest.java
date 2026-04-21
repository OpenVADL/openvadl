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

package vadl.utils;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ForIdxNode;

public class GraphUtilsTest {

  @Test
  public void copyWithNodeSubstitutionUsesReplacementForMatchingNodes() {
    var idx = new ForIdxNode(Type.bits(8), 0, 7);
    var shared = BuiltInTable.ADD.call(idx, bits(1));
    var root = BuiltInTable.XOR.call(shared, shared);
    var replacement = new ForIdxNode(Type.bits(8), 0, 7);

    var copy = (BuiltInCall) GraphUtils.copyWithNodeSubstitution(root,
        node -> node == shared ? replacement : null);

    assertNotSame(root, copy);
    assertSame(replacement, copy.arguments().get(0));
    assertSame(replacement, copy.arguments().get(1));
  }

  @Test
  public void copyWithNodeSubstitutionPreservesSharedCopiedSubgraphs() {
    var idx = new ForIdxNode(Type.bits(8), 0, 7);
    var shared = BuiltInTable.ADD.call(idx, bits(1));
    var root = BuiltInTable.XOR.call(shared, shared);

    var copy = (BuiltInCall) GraphUtils.copyWithNodeSubstitution(root, node -> null);

    assertNotSame(root, copy);
    assertNotSame(shared, copy.arguments().get(0));
    assertSame(copy.arguments().get(0), copy.arguments().get(1));
  }

  private static ConstantNode bits(long value) {
    return Constant.Value.of(value, Type.bits(8)).toNode();
  }
}
