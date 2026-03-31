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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.utils.GraphUtils.truncate;
import static vadl.viam.helper.TestGraphUtils.binaryOp;
import static vadl.viam.helper.TestGraphUtils.bits;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.visualize.DotGraphVisualizer;
import vadl.viam.helper.TestGraph;
import vadl.viam.passes.canonicalization.Canonicalizer;

public class CanonicalizerTest {

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
  void constantEvaluation() {
    testGraph.addWithInputs(
        binaryOp(BuiltInTable.ADD,
            bits(5, 10),
            bits(10, 10))
    );


    testGraph.verify();

    assertEquals(3, testGraph.getNodes().count());

    Canonicalizer.canonicalize(testGraph);

    assertEquals(1, testGraph.getNodes().count());
    assertEquals(15,
        ((Constant.Value) ((ConstantNode) testGraph.getNodes().findFirst()
            .get()).constant()).integer().intValue());
  }

  @Test
  void constantEvaluationWithCast() {

    var addition = testGraph.addWithInputs(
        binaryOp(BuiltInTable.ADD,
            bits(5, 10),
            bits(-10, 10))
    );
    testGraph.add(truncate(addition, Type.unsignedInt(5)));

    testGraph.verify();

    assertEquals(4, testGraph.getNodes().count());

    Canonicalizer.canonicalize(testGraph);

    assertEquals(1, testGraph.getNodes().count());
    assertEquals(27,
        ((Constant.Value) ((ConstantNode) testGraph.getNodes().findFirst()
            .get()).constant()).integer().intValue());

    testGraph.verify();
  }

  @Test
  void constantEvaluationSubGraph() {

    var addition = testGraph.addWithInputs(
        binaryOp(BuiltInTable.ADD,
            bits(5, 10),
            bits(-10, 10))
    );
    testGraph.add(truncate(addition, Type.unsignedInt(5)));

    testGraph.verify();

    assertEquals(4, testGraph.getNodes().count());

    var result = Canonicalizer.canonicalizeSubGraph(addition);

    assertEquals(2, testGraph.getNodes().count());
    assertEquals(1019,
        ((Constant.Value) testGraph.getNodes(ConstantNode.class).findFirst()
            .get().constant()).integer().intValue());
    assertEquals(1019,
        ((Constant.Value) ((ConstantNode) result).constant()).integer().intValue());

    testGraph.verify();
  }

  @Test
  void constantEvaluationSubGraph_simpleConstant() {

    var t = testGraph.add(new ConstantNode(bits(5, 10)));

    testGraph.verify();

    assertEquals(1, testGraph.getNodes().count());

    var result = Canonicalizer.canonicalizeSubGraph(t);

    assertEquals(1, testGraph.getNodes().count());
    assertEquals(5,
        ((Constant.Value) testGraph.getNodes(ConstantNode.class).findFirst()
            .get().constant()).integer().intValue());
    assertEquals(5,
        ((Constant.Value) ((ConstantNode) result).constant()).integer().intValue());

    testGraph.verify();
  }


}
