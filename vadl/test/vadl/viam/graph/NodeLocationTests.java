// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.utils.SourceLocation;
import vadl.viam.helper.TestGraph;
import vadl.viam.helper.TestNodes.Plain;
import vadl.viam.helper.TestNodes.WithInput;
import vadl.viam.helper.TestNodes.WithTwoInputs;

/**
 * Tests for {@link Node#setSourceLocationRecursively(SourceLocation)}.
 */
public class NodeLocationTests {

  private static final SourceLocation LOC =
      new SourceLocation.DirectLocation(Path.of("test.vadl"), 1, 2);

  private static final SourceLocation OTHER_LOC =
      new SourceLocation.DirectLocation(Path.of("other.vadl"), 5, 6);

  TestGraph testGraph;

  @BeforeEach
  public void setUp() {
    testGraph = new TestGraph("LocationTestGraph");
  }

  @Test
  void setSourceLocationRecursively_singleNode_setsLocation() {
    var node = new Plain();
    testGraph.add(node);

    node.setSourceLocationRecursively(LOC);

    assertEquals(LOC, node.location());
  }

  @Test
  void setSourceLocationRecursively_nodeWithInput_setsLocationOnBoth() {
    var input = new Plain();
    testGraph.add(input);
    var node = new WithInput(input);
    testGraph.add(node);

    node.setSourceLocationRecursively(LOC);

    assertEquals(LOC, node.location());
    assertEquals(LOC, input.location());
  }

  @Test
  void setSourceLocationRecursively_deepChain_setsLocationOnAll() {
    var leaf = new Plain();
    testGraph.add(leaf);
    var middle = new WithInput(leaf);
    testGraph.add(middle);
    var root = new WithInput(middle);
    testGraph.add(root);

    root.setSourceLocationRecursively(LOC);

    assertEquals(LOC, root.location());
    assertEquals(LOC, middle.location());
    assertEquals(LOC, leaf.location());
  }

  @Test
  void setSourceLocationRecursively_nodeAlreadyHasLocation_skipsNodeAndItsInputs() {
    // leaf has no location yet, but middle already has OTHER_LOC set.
    // After recursive application from root, middle should keep OTHER_LOC and
    // leaf should remain with no location.
    var leaf = new Plain();
    testGraph.add(leaf);
    var middle = new WithInput(leaf);
    testGraph.add(middle);
    middle.setSourceLocation(OTHER_LOC);   // pre-existing location
    var root = new WithInput(middle);
    testGraph.add(root);

    root.setSourceLocationRecursively(LOC);

    assertEquals(LOC, root.location());
    // middle already had a location, recursion stops here
    assertEquals(OTHER_LOC, middle.location());
    // leaf is below middle, so it must NOT be touched
    assertTrue(leaf.location().equals(SourceLocation.INVALID_SOURCE_LOCATION),
        "leaf should have no location because recursion was stopped at middle");
  }

  @Test
  void setSourceLocationRecursively_nodeWithTwoInputs_setsLocationOnAllBranches() {
    var input1 = new Plain();
    testGraph.add(input1);
    var input2 = new Plain();
    testGraph.add(input2);
    var root = new WithTwoInputs(input1, input2);
    testGraph.add(root);

    root.setSourceLocationRecursively(LOC);

    assertEquals(LOC, root.location());
    assertEquals(LOC, input1.location());
    assertEquals(LOC, input2.location());
  }

  @Test
  void setSourceLocationRecursively_rootAlreadyHasLocation_doesNotOverride() {
    var node = new Plain();
    testGraph.add(node);
    node.setSourceLocation(OTHER_LOC);

    node.setSourceLocationRecursively(LOC);

    // Root already had a location; it should not be changed
    assertEquals(OTHER_LOC, node.location());
  }

  @Test
  void setSourceLocationRecursively_oneInputAlreadyHasLocation_onlyThatBranchSkipped() {
    var leafWithLoc = new Plain();
    testGraph.add(leafWithLoc);
    leafWithLoc.setSourceLocation(OTHER_LOC);  // pre-existing

    var leafWithoutLoc = new Plain();
    testGraph.add(leafWithoutLoc);

    var root = new WithTwoInputs(leafWithLoc, leafWithoutLoc);
    testGraph.add(root);

    root.setSourceLocationRecursively(LOC);

    assertEquals(LOC, root.location());
    // This branch already had a location, so it should not be changed
    assertEquals(OTHER_LOC, leafWithLoc.location());
    // This branch had no location, so it should be updated
    assertEquals(LOC, leafWithoutLoc.location());
  }
}
