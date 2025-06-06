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

package vadl.viam.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.viam.graph.GraphMatchers.activeIn;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.viam.graph.visualize.DotGraphVisualizer;
import vadl.viam.helper.TestGraph;
import vadl.viam.helper.TestNodes;
import vadl.viam.helper.TestNodes.Plain;
import vadl.viam.helper.TestNodes.PlainUnique;
import vadl.viam.helper.TestNodes.WithDataUnique;
import vadl.viam.helper.TestNodes.WithInputUnique;
import vadl.viam.helper.TestNodes.WithTwoInputs;
import vadl.viam.helper.TestNodes.WithTwoInputsUnique;


/**
 * The GraphBuildingTests class is a test class that contains several test
 * methods for validating the functionality of the Graph class.
 * It performs tests related to adding nodes to the graph and checking node duplication.
 *
 * @see Graph
 * @see Node
 */
public class GraphBuildingTests {

  TestGraph testGraph;

  @BeforeEach
  public void setUp() {
    testGraph = new TestGraph("VerificationTestGraph");
  }

  @AfterEach
  public void tearDown() {
    System.out.println(
        new DotGraphVisualizer()
            .load(testGraph)
            .visualize()
    );
  }

  @Test
  void add_SingleNode_Success() {
    var node = testGraph.add(new Plain());
    assertNotNull(node);
    assertThat(testGraph.getNodes().count(), equalTo(1L));
  }

  @Test
  void add_MultipleNodes_Success() {
    testGraph.add(new Plain());
    testGraph.add(new Plain());
    testGraph.add(new Plain());
    assertThat(testGraph.getNodes().count(), equalTo(3L));

    assertThat(testGraph.getNodes().toList(), everyItem(activeIn(testGraph)));
  }

  @Test
  void add_MultipleUniqueNodes_Success() {
    testGraph.add(new PlainUnique());
    testGraph.add(new PlainUnique());
    testGraph.add(new PlainUnique());
    assertThat(testGraph.getNodes().count(), equalTo(1L));

    assertThat(testGraph.getNodes().toList(), everyItem(activeIn(testGraph)));
  }

  @Test
  void add_WithUniqueInputsNested_Success() {
    var d1 = new WithDataUnique(1);
    var d2 = new WithDataUnique(2);
    d1 = testGraph.add(d1);
    d2 = testGraph.add(d2);
    var n1 = new WithTwoInputsUnique(d1, d2);
    n1 = testGraph.add(n1);


    var n2 = new WithTwoInputsUnique(
        new WithDataUnique(1),
        new WithDataUnique(2)
    );
    var n2return = testGraph.addWithInputs(n2);


    assertNotEquals(n2, n2return);
    assertEquals(n1, n2return);
    assertEquals(n1.inputs().toList(), n2.inputs().toList());
  }

  @Test
  void add_WithUniqueInputsNested2_Success() {
    var d1 = new WithDataUnique(1);
    var d2 = new WithDataUnique(2);
    d1 = testGraph.add(d1);
    d2 = testGraph.add(d2);
    var n1 = new WithTwoInputsUnique(d1, d2);
    n1 = testGraph.add(n1);


    var n2 = new WithTwoInputs(
        new WithDataUnique(1),
        new WithDataUnique(2)
    );
    var n2return = testGraph.addWithInputs(n2);


    assertEquals(n2, n2return);
    assertEquals(n1.inputs().toList(), n2.inputs().toList());
  }

  @Test
  void add_twoTimes_Failure() {
    var n = testGraph.add(new Plain());
    var exc = assertThrows(ViamGraphError.class, () -> testGraph.add(n));
    assertThat(exc.getMessage(), containsString("node is not uninitialized"));
  }

  @Test
  void add_twoTimesUnique_Failure() {
    var n = testGraph.add(new PlainUnique());
    var exc = assertThrows(ViamGraphError.class, () -> testGraph.add(n));
    assertThat(exc.getMessage(), containsString("node is not uninitialized"));
  }

  @Test
  void add_withNodeListInput_Failure() {
    var inputs = new NodeList<Node>(
        new WithInputUnique(new WithDataUnique(1)),
        new WithInputUnique(new WithDataUnique(1))
    );

    testGraph.addWithInputs(new TestNodes.WithNodeListInput(inputs));
    assertEquals(3, testGraph.getNodes().count());
  }

  @Test
  void copy_UniqueNode_Success() {
    testGraph.add(new PlainUnique());

    var copiedTestGraph = testGraph.copy();

    assertThat(testGraph.getNodes().count(), equalTo(1L));
    assertThat(copiedTestGraph.getNodes().count(), equalTo(1L));
    assertNotSame(testGraph, copiedTestGraph);
    assertNotSame(testGraph.getNodes().findFirst().get(),
        copiedTestGraph.getNodes().findFirst().get());
  }

  @Test
  void copy_MultipleDataNode_Success() {
    final var p1 = testGraph.add(new Plain());
    final var p2 = testGraph.add(new Plain());
    final var x = testGraph.add(new WithTwoInputs(p1, p2));

    var copiedTestGraph = testGraph.copy();
    final var y = (WithTwoInputs) copiedTestGraph.getNodes(WithTwoInputs.class).findFirst().get();

    assertThat(testGraph.getNodes().count(), equalTo(3L));
    assertThat(copiedTestGraph.getNodes().count(), equalTo(3L));
    assertNotSame(testGraph, copiedTestGraph);
    assertNotSame(x, y);
    assertNotSame(x.input1, y.input1);
    assertNotSame(x.input2, y.input2);
  }

  @Test
  void replaceNode_Success() {
    testGraph.add(new Plain());
    var replace = testGraph.add(new Plain());
    testGraph.add(new Plain());
    var newNode = new Plain();
    assertThat(testGraph.getNodes().count(), equalTo(3L));

    replace.replaceAndDelete(newNode);

    assertThat(testGraph.getNodes().count(), equalTo(3L));
    assertTrue(replace.isDeleted());
  }

  @Test
  void replaceNodeWithInputs_Success() {
    testGraph.add(new Plain());
    var replace = testGraph.addWithInputs(new TestNodes.WithTwoInputs(new Plain(), new Plain()));
    testGraph.add(new Plain());
    var newNode = new Plain();
    assertThat(testGraph.getNodes().count(), equalTo(5L));

    testGraph.verify();

    replace.replaceAndDelete(newNode);

    testGraph.verify();
    assertThat(testGraph.getNodes().count(), equalTo(3L));
    assertTrue(replace.isDeleted());
  }

  @Test
  void replaceNodeWithSuccessor_Success() {
    var second = testGraph.add(new Plain());
    var replace = testGraph.add(new TestNodes.WithSuccessor(second));
    testGraph.add(new Plain());
    var newNode = new Plain();
    assertThat(testGraph.getNodes().count(), equalTo(3L));

    replace.replaceAndDelete(newNode);

    assertThat(testGraph.getNodes().count(), equalTo(2L));
    assertTrue(replace.isDeleted());
    assertTrue(second.isDeleted());
  }
}
